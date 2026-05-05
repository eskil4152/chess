package com.blikeng.chess.service;

import com.blikeng.chess.dto.websocket.WsDrawDTO;
import com.blikeng.chess.dto.websocket.WsGameStateDTO;
import com.blikeng.chess.dto.websocket.WsMoveDTO;
import com.blikeng.chess.engine.MoveExecutor;
import com.blikeng.chess.engine.PositionMapper;
import com.blikeng.chess.entity.GameEntity;
import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.errorTypes.GameNotFoundException;
import com.blikeng.chess.exception.errorTypes.InvalidMoveException;
import com.blikeng.chess.exception.errorTypes.InvalidUUIDException;
import com.blikeng.chess.model.*;
import com.blikeng.chess.model.piece.PieceType;
import com.blikeng.chess.notifications.NotificationService;
import com.blikeng.chess.notifications.events.MatchEndedEvent;
import com.blikeng.chess.notifications.events.MatchStartedEvent;
import com.blikeng.chess.notifications.events.MoveMadeEvent;
import com.blikeng.chess.repository.GameRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class GameService {
    private final GameRepository gameRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;
    private final UserService userService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MoveExecutor moveExecutor = new MoveExecutor();
    private final Logger logger = LoggerFactory.getLogger(GameService.class);

    private final ConcurrentHashMap<UUID, Game> games = new ConcurrentHashMap<>();

    public GameService(
            GameRepository gameRepository,
            ApplicationEventPublisher eventPublisher,
            NotificationService notificationService,
            UserService userService) {
        this.gameRepository = gameRepository;
        this.eventPublisher = eventPublisher;
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @Transactional
    public void beginGame(UserEntity whitePlayer, UserEntity blackPlayer) {
        GameEntity gameEntity = gameRepository.save(new GameEntity(
                whitePlayer,
                blackPlayer,
                GameStatus.ONGOING,
                Instant.now()
        ));

        Game game = new Game(gameEntity.getId(), whitePlayer.getId(), whitePlayer.getUsername(), blackPlayer.getId(), blackPlayer.getUsername());
        game.setWhiteKingPosition(new Position(0, 4));
        game.setBlackKingPosition(new Position(7, 4));

        games.put(game.getId(), game);

        eventPublisher.publishEvent(new MatchStartedEvent(
                game.getId(),
                whitePlayer.getId(),
                whitePlayer.getUsername(),
                blackPlayer.getId(),
                blackPlayer.getUsername()
        ));

        logger.info("Game started: {}. White: {}. Black: {}", game.getId(), whitePlayer.getUsername(), blackPlayer.getUsername());
    }

    @Transactional
    public void makeMove(UUID userId, WsMoveDTO moveDTO) {
        System.out.println("Got a move. " + moveDTO.move());

        Game game = getGame(moveDTO.gameId()).orElseThrow(GameNotFoundException::new);

        ReentrantLock lock = game.lockGame();
        lock.lock();

        try {
            if (!isUserTurn(game, userId)) return;

            if (moveDTO.move().length() < 4) throw new InvalidMoveException();

            String move = moveDTO.move();
            String from = move.substring(0, 2);
            String to = move.substring(2, 4);
            PieceType promotion = move.length() > 4 ? PieceType.fromChar(move.charAt(4)) : null;

            GameStatus gameStatus = moveExecutor.performMove(
                    game,
                    new Move(PositionMapper.fromString(from), PositionMapper.fromString(to)),
                    promotion
            );

            if (gameStatus == GameStatus.ONGOING) {
                game.addMove(moveDTO.move());

                game.setWhiteDraw(false);
                game.setBlackDraw(false);

                eventPublisher.publishEvent(new MoveMadeEvent(
                        game.getId(), game.getWhiteId(), game.getBlackId(), moveDTO.move()
                ));
            } else if (gameStatus != null) {
                game.addMove(moveDTO.move());

                EndedBy endedBy;
                if (gameStatus == GameStatus.DRAW)  endedBy = EndedBy.STALEMATE;
                else endedBy = EndedBy.CHECKMATE;

                handleGameEnd(game, gameStatus, endedBy);
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean isInGame(UUID userId) {
        return games.values().stream()
                .anyMatch(g -> g.getWhiteId().equals(userId) || g.getBlackId().equals(userId));
    }

    public Optional<Game> getActiveGame(UUID userId) {
        return games.values().stream()
                .filter(g -> g.getWhiteId().equals(userId) || g.getBlackId().equals(userId))
                .findFirst();
    }

    public void onSessionConnected(UUID userId, WebSocketSession session) {
        games.values().stream()
                .filter(g -> g.getWhiteId().equals(userId) || g.getBlackId().equals(userId))
                .findFirst()
                .ifPresent(game -> {
                    try {
                        String payload = objectMapper.writeValueAsString(new WsGameStateDTO(
                                game.getId(),
                                game.getWhiteId(),
                                game.getWhiteUsername(),
                                game.getBlackId(),
                                game.getBlackUsername(),
                                game.getMoves(),
                                game.isWhiteDraw(),
                                game.isBlackDraw()
                        ));
                        notificationService.sendToSession(session, payload);
                    } catch (JsonProcessingException e) {
                        logger.error("Error serializing game state for game {}: ", game.getId(), e);
                    }
                });
    }

    private boolean isUserTurn(Game game, UUID userId) {
        return game.isWhiteTurn() ? game.getWhiteId().equals(userId) : game.getBlackId().equals(userId);
    }

    private Optional<Game> getGame(String gameString) {
        try {
            return Optional.ofNullable(games.get(UUID.fromString(gameString)));
        } catch (IllegalArgumentException _) {
            throw new InvalidUUIDException();
        }
    }

    private void handleGameEnd(Game game, GameStatus gameStatus, EndedBy endedBy) {
        gameRepository.findById(game.getId()).ifPresent(entity -> {
            entity.setMoves(game.getMoves());
            entity.setStatus(gameStatus);
            gameRepository.save(entity);
        });

        eventPublisher.publishEvent(new MatchEndedEvent(game.getId(), game.getWhiteId(), game.getBlackId(), gameStatus, endedBy));
        games.remove(game.getId());

        userService.updateUserElo(game.getWhiteId(), game.getBlackId(), gameStatus);

        logger.info("Game ended: {}. Black: {}. White: {}. Result: {}", game.getId(), game.getWhiteUsername(), game.getBlackUsername(), gameStatus.name());
    }
}
