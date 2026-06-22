package com.blikeng.chess.service.game;

import com.blikeng.chess.dto.websocket.*;
import com.blikeng.chess.engine.MoveExecutor;
import com.blikeng.chess.engine.PositionMapper;
import com.blikeng.chess.exception.ApiException;
import com.blikeng.chess.exception.types.*;
import com.blikeng.chess.model.*;
import com.blikeng.chess.events.MoveMadeEvent;
import com.blikeng.chess.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Core handling of an in-progress game's actions: making moves, resigning, and draw
 * offers/agreement.
 *
 * <p>Each action locks the {@link Game} for its duration. Lookups go through
 * {@link ActiveGameStore}, clocks through {@link GameClockService}, and ending a game is
 * delegated to {@link GameCompletionService}. Game creation and state queries live in
 * {@link GameCreationService} and {@link GameViewService}.
 */
@Service
public class GameService {
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;
    private final GameClockService gameClockService;
    private final GameCompletionService gameCompletionService;
    private final ActiveGameStore activeGameStore;
    private final RedisTemplate<String, String> redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MoveExecutor moveExecutor = new MoveExecutor();
    private final Logger logger = LoggerFactory.getLogger(GameService.class);

    public GameService(
            ApplicationEventPublisher eventPublisher,
            NotificationService notificationService,
            GameClockService gameClockService,
            GameCompletionService gameCompletionService,
            ActiveGameStore activeGameStore,
            RedisTemplate<String, String> redisTemplate
    ){
        this.eventPublisher = eventPublisher;
        this.notificationService = notificationService;
        this.gameClockService = gameClockService;
        this.gameCompletionService = gameCompletionService;
        this.activeGameStore = activeGameStore;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public void makeMove(UUID userId, WsMoveDTO moveDTO) {
        Game game = activeGameStore.get(moveDTO.gameId()).orElse(null);

        if (game != null){
            ReentrantLock lock = game.lockGame();
            lock.lock();

            try {
                if (!isUserTurn(game, userId)) return;

                boolean isWhite = game.getWhiteId().equals(userId);
                if (!game.isBotGame() && gameClockService.handleTime(game, isWhite)) {
                    GameStatus flagStatus = isWhite ? GameStatus.BLACK_WIN : GameStatus.WHITE_WIN;
                    gameCompletionService.endGame(game, flagStatus);
                    return;
                }

                if (moveDTO.move().length() < 4) throw new InvalidMoveException();

                String move = moveDTO.move();

                GameStatus gameStatus = moveExecutor.performMove(
                    game,
                    PositionMapper.fromUci(move)
                );

                if (gameStatus == GameStatus.ONGOING) {
                    game.addMove(moveDTO.move());

                    game.setWhiteDraw(false);
                    game.setBlackDraw(false);

                    if (!game.isBotGame()) {
                        int increment = game.getTimeControl().incrementMs();
                        if (isWhite) game.setWhiteRemainingMs(game.getWhiteRemainingMs() + increment);
                        else game.setBlackRemainingMs(game.getBlackRemainingMs() + increment);

                        game.setTurnStartTime(System.currentTimeMillis());
                        gameClockService.scheduleFlagCheck(game, !isWhite, gameCompletionService::endGame);

                        eventPublisher.publishEvent(new MoveMadeEvent(
                            game.getId(), game.getWhiteId(), game.getBlackId(), moveDTO.move(), game.isWhiteTurn(), game.getTimeControl().incrementMs(), game.getSpectators()
                        ));
                    } else {
                        eventPublisher.publishEvent(new MoveMadeEvent(
                            game.getId(), game.getWhiteId(), game.getBlackId(), moveDTO.move(), game.isWhiteTurn(), 0, game.getSpectators()
                        ));
                    }
                } else if (gameStatus != null) {
                    game.addMove(moveDTO.move());

                    gameCompletionService.endGame(game, gameStatus);
                }
            } finally {
                lock.unlock();
            }
        } else {
            var command = Map.of(
                "action", "MOVE",
                "userId", userId.toString(),
                "gameId", moveDTO.gameId(),
                "move", moveDTO.move()
            );

            try {
                redisTemplate.convertAndSend("game:" + moveDTO.gameId(), objectMapper.writeValueAsString(command));
            } catch (JacksonException e) {
                logger.error("Failed to serialize message: {} when making move in game {}", command, moveDTO.gameId(), e);
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to make move");
            }
        }
    }

    @Transactional
    public void resignGame(UUID userId, WsResignDTO resignDTO) {
        Game game = activeGameStore.get(resignDTO.gameId()).orElse(null);

        if (game != null){
            ReentrantLock lock = game.lockGame();
            lock.lock();

            try {
                if (!game.getWhiteId().equals(userId) && !game.getBlackId().equals(userId)) {
                    throw new NotAllowedException();
                }

                boolean isWhite = game.getWhiteId().equals(userId);
                GameStatus gameStatus = isWhite ? GameStatus.BLACK_WIN : GameStatus.WHITE_WIN;
                game.setEndedBy(EndedBy.RESIGNATION);
                gameCompletionService.endGame(game, gameStatus);
            } finally {
                lock.unlock();
            }
        } else {
            var command = Map.of(
                "action", "RESIGN",
                "userId", userId.toString(),
                "gameId", resignDTO.gameId()
            );

            try {
                redisTemplate.convertAndSend("game:" + resignDTO.gameId(), objectMapper.writeValueAsString(command));
            } catch (JacksonException e) {
                logger.error("Failed to serialize message: {} when resigning game {} as {}", command, resignDTO.gameId(), userId, e);
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to resign game");
            }
        }
    }

    @Transactional
    public void handleDraw(UUID userId, WsDrawDTO drawDTO) {
        Game game = activeGameStore.get(drawDTO.gameId()).orElse(null);

        if (game != null){
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
                    game.setEndedBy(EndedBy.AGREEMENT);
                    gameCompletionService.endGame(game, GameStatus.DRAW);
                } else {
                    UUID otherUser = isWhite ? game.getBlackId() : game.getWhiteId();
                    notificationService.sendDrawOffer(game.getId(), otherUser);
                }
            } finally {
                lock.unlock();
            }
        } else {
            var command = Map.of(
                "action", "DRAW",
                "userId", userId.toString(),
                "gameId", drawDTO.gameId()
            );

            try {
                redisTemplate.convertAndSend("game:" + drawDTO.gameId(), objectMapper.writeValueAsString(command));
            } catch (JacksonException e) {
                logger.error("Failed to serialize message: {} when sending draw in game {} as {}", command, drawDTO.gameId(), userId, e);
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to offer draw");
            }
        }
    }

    private boolean isUserTurn(Game game, UUID userId) {
        return game.isWhiteTurn() ? game.getWhiteId().equals(userId) : game.getBlackId().equals(userId);
    }
}
