package com.blikeng.chess.service;

import com.blikeng.chess.dto.websocket.WsDrawDTO;
import com.blikeng.chess.dto.GameStateDTO;
import com.blikeng.chess.dto.websocket.WsMoveDTO;
import com.blikeng.chess.dto.websocket.WsResignDTO;
import com.blikeng.chess.engine.MoveExecutor;
import com.blikeng.chess.engine.PositionMapper;
import com.blikeng.chess.entity.GameEntity;
import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.errorTypes.*;
import com.blikeng.chess.model.*;
import com.blikeng.chess.model.piece.PieceType;
import com.blikeng.chess.notifications.NotificationService;
import com.blikeng.chess.notifications.events.MatchEndedEvent;
import com.blikeng.chess.notifications.events.MatchStartedEvent;
import com.blikeng.chess.notifications.events.MoveMadeEvent;
import com.blikeng.chess.repository.GameRepository;
import com.blikeng.chess.security.JwtPrincipal;
import com.blikeng.chess.security.JwtService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public void beginGame(UserEntity player1, UserEntity player2) {
        UserEntity whitePlayer;
        UserEntity blackPlayer;

        double random = Math.random();
        if (random < 0.5) {
            whitePlayer = player1;
            blackPlayer = player2;
        } else {
            whitePlayer = player2;
            blackPlayer = player1;
        }

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
                blackPlayer.getUsername(),
                whitePlayer.getElo(),
                blackPlayer.getElo()
        ));

        logger.info("Game started: {}. White: {}. Black: {}", game.getId(), whitePlayer.getUsername(), blackPlayer.getUsername());
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

                game.setWhiteDraw(false);
                game.setBlackDraw(false);

                eventPublisher.publishEvent(new MoveMadeEvent(
                        game.getId(), game.getWhiteId(), game.getBlackId(), moveDTO.move()
                ));
            } else if (gameStatus != null) {
                game.addMove(moveDTO.move());

                EndedBy endedBy;
                if (gameStatus == GameStatus.DRAW) endedBy = EndedBy.STALEMATE;
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

    public GameStateDTO restoreGameState() {
        JwtPrincipal jwtPrincipal = JwtService.getCurrentUser();
        if (jwtPrincipal == null) throw new InvalidUserException();

        UUID userId = jwtPrincipal.userId();

        GameStateDTO gameNullable =  games.values().stream()
                .filter(g -> g.getWhiteId().equals(userId) || g.getBlackId().equals(userId))
                .findFirst()
                .map(game -> new GameStateDTO(
                        game.getId(),
                        game.getWhiteId(),
                        game.getWhiteUsername(),
                        game.getBlackId(),
                        game.getBlackUsername(),
                        game.getMoves(),
                        game.isWhiteDraw(),
                        game.isBlackDraw()
                ))
                .orElse(null);

        if (gameNullable == null) throw new GameNotFoundException();
        else return gameNullable;
    }

    @Transactional
    public void resignGame(UUID userId, WsResignDTO resignDTO) {
        Game game = getGame(resignDTO.gameId()).orElseThrow(GameNotFoundException::new);

        ReentrantLock lock = game.lockGame();
        lock.lock();

        try {
            if (!game.getWhiteId().equals(userId) && !game.getBlackId().equals(userId)) {
                throw new NotAllowedException();
            }

            boolean isWhite = game.getWhiteId().equals(userId);
            GameStatus gameStatus = isWhite ? GameStatus.BLACK_WIN : GameStatus.WHITE_WIN;
            handleGameEnd(game, gameStatus, EndedBy.RESIGNATION);
        } finally {
            lock.unlock();
        }
    }

    @Transactional
    public void handleDraw(UUID userId, WsDrawDTO drawDTO) {
        Game game = getGame(drawDTO.gameId()).orElseThrow(GameNotFoundException::new);

        ReentrantLock lock = game.lockGame();
        lock.lock();

        try {
            if (!game.getWhiteId().equals(userId) && !game.getBlackId().equals(userId)) {
                throw new NotAllowedException();
            }

            boolean isWhite = game.getWhiteId().equals(userId);

            if (isWhite) game.setWhiteDraw(true);
            else game.setBlackDraw(true);

            if (game.isWhiteDraw() && game.isBlackDraw()) {
                handleGameEnd(game, GameStatus.DRAW, EndedBy.AGREEMENT);
            } else {
                UUID otherUser = isWhite ? game.getBlackId() : game.getWhiteId();
                notificationService.sendDrawOffer(game.getId(), otherUser);
            }
        } finally {
            lock.unlock();
        }
    }

    private void handleGameEnd(Game game, GameStatus gameStatus, EndedBy endedBy) {
        gameRepository.findById(game.getId()).ifPresent(entity -> {
            entity.setMoves(game.getMoves());
            entity.setStatus(gameStatus);
            gameRepository.save(entity);
        });

        if (!game.getMoves().isEmpty()) {
            eventPublisher.publishEvent(new MoveMadeEvent(
                    game.getId(), game.getWhiteId(), game.getBlackId(), game.getMoves().getLast()
            ));
        }

        int[] newElo = userService.updateUserElo(game.getWhiteId(), game.getBlackId(), gameStatus);

        eventPublisher.publishEvent(new MatchEndedEvent(game.getId(), game.getWhiteId(), game.getBlackId(), gameStatus, endedBy, newElo[0], newElo[1]));
        games.remove(game.getId());

        logger.info("Game ended: {}. Black: {}. White: {}. Result: {}", game.getId(), game.getWhiteUsername(), game.getBlackUsername(), gameStatus.name());
    }

    private Optional<Game> getGame(String gameString) {
        try {
            return Optional.ofNullable(games.get(UUID.fromString(gameString)));
        } catch (IllegalArgumentException _) {
            throw new InvalidUUIDException();
        }
    }

    private boolean isUserTurn(Game game, UUID userId) {
        return game.isWhiteTurn() ? game.getWhiteId().equals(userId) : game.getBlackId().equals(userId);
    }
}
