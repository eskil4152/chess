package com.blikeng.chess.service;

import com.blikeng.chess.dto.websocket.WsGameStartedDTO;
import com.blikeng.chess.dto.websocket.WsMoveDTO;
import com.blikeng.chess.engine.MoveExecutor;
import com.blikeng.chess.engine.PositionMapper;
import com.blikeng.chess.entity.GameEntity;
import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.ErrorTypes.GameNotFoundException;
import com.blikeng.chess.exception.ErrorTypes.InvalidMoveException;
import com.blikeng.chess.model.*;
import com.blikeng.chess.model.piece.PieceType;
import com.blikeng.chess.notifications.NotificationService;
import com.blikeng.chess.notifications.events.MatchEndedEvent;
import com.blikeng.chess.notifications.events.MatchStartedEvent;
import com.blikeng.chess.notifications.events.MoveMadeEvent;
import com.blikeng.chess.repository.GameRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MoveExecutor moveExecutor = new MoveExecutor();
    private final ConcurrentHashMap<UUID, Game> games = new ConcurrentHashMap<>();
    private final UserService userService;

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
        Game game = new Game(whitePlayer.getId(), whitePlayer.getUsername(), blackPlayer.getId(), blackPlayer.getUsername());
        game.setWhiteKingPosition(new Position(7, 4));
        game.setBlackKingPosition(new Position(0, 4));

        gameRepository.save(new GameEntity(
                game.getId(),
                whitePlayer,
                blackPlayer,
                GameStatus.ONGOING,
                Instant.now()
        ));

        games.put(game.getId(), game);

        eventPublisher.publishEvent(new MatchStartedEvent(
                game.getId(),
                whitePlayer.getId(),
                whitePlayer.getUsername(),
                blackPlayer.getId(),
                blackPlayer.getUsername()
        ));
    }

    @Transactional
    public void makeMove(UUID userId, WsMoveDTO moveDTO) {
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

                eventPublisher.publishEvent(new MoveMadeEvent(
                        game.getId(), game.getWhiteId(), game.getBlackId(), moveDTO.move()
                ));
            } else if (gameStatus != null) {
                game.addMove(moveDTO.move());

                handleGameEnd(game, gameStatus);
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean isInGame(UUID userId) {
        return games.values().stream()
                .anyMatch(g -> g.getWhiteId().equals(userId) || g.getBlackId().equals(userId));
    }

    public void onSessionConnected(UUID userId, WebSocketSession session) {
        games.values().stream()
                .filter(g -> g.getWhiteId().equals(userId) || g.getBlackId().equals(userId))
                .findFirst()
                .ifPresent(game -> {
                    try {
                        // TODO: replace with WsGameStateDTO once move history is implemented. Currently user just receives a fresh game, not the actual game state
                        String payload = objectMapper.writeValueAsString(new WsGameStartedDTO(
                                game.getId(),
                                game.getWhiteId(),
                                game.getWhiteUsername(),
                                game.getBlackId(),
                                game.getBlackUsername()
                        ));
                        notificationService.sendToSession(session, payload);
                    } catch (JsonProcessingException e) {
                        // session will need to retry
                    }
                });
    }

    private boolean isUserTurn(Game game, UUID userId) {
        return game.isWhiteTurn() ? game.getWhiteId().equals(userId) : game.getBlackId().equals(userId);
    }

    private Optional<Game> getGame(String gameString) {
        try {
            return Optional.ofNullable(games.get(UUID.fromString(gameString)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private void handleGameEnd(Game game, GameStatus gameStatus) {
        gameRepository.updateGameById(game.getId(), game.getMoves(), gameStatus);
        eventPublisher.publishEvent(new MatchEndedEvent(game.getId(), game.getWhiteId(), game.getBlackId(), gameStatus));
        games.remove(game.getId());

        userService.updateUserElo(game.getWhiteId(), game.getBlackId(), game.getStatus());
    }
}
