package com.blikeng.chess.service;

import com.blikeng.chess.dto.GameStateDTO;
import com.blikeng.chess.dto.websocket.*;
import com.blikeng.chess.engine.MoveExecutor;
import com.blikeng.chess.engine.PositionMapper;
import com.blikeng.chess.engine.converter.PgnConverter;
import com.blikeng.chess.bot.BotDefinition;
import com.blikeng.chess.entity.GameEntity;
import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.types.*;
import com.blikeng.chess.model.*;
import com.blikeng.chess.model.timecontrol.TimeControl;
import com.blikeng.chess.notifications.NotificationService;
import com.blikeng.chess.notifications.events.MatchEndedEvent;
import com.blikeng.chess.notifications.events.MatchStartedEvent;
import com.blikeng.chess.notifications.events.MoveMadeEvent;
import com.blikeng.chess.repository.GameRepository;
import com.blikeng.chess.security.JwtPrincipal;
import com.blikeng.chess.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class GameService {
    private final GameRepository gameRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;
    private final UserService userService;

    private final MoveExecutor moveExecutor = new MoveExecutor();
    private final Logger logger = LoggerFactory.getLogger(GameService.class);

    private final ConcurrentHashMap<UUID, Game> games = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final ConcurrentHashMap<UUID, ScheduledFuture<?>> flagTasks = new ConcurrentHashMap<>();

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
    public void beginGame(UserEntity player1, UserEntity player2, TimeControl timeControl) {
        UserEntity whitePlayer;
        UserEntity blackPlayer;

        if (ThreadLocalRandom.current().nextBoolean()) {
            whitePlayer = player1;
            blackPlayer = player2;
        } else {
            whitePlayer = player2;
            blackPlayer = player1;
        }

        Instant startTime = Instant.now();

        GameEntity gameEntity = gameRepository.save(new GameEntity(
                whitePlayer,
                blackPlayer,
                GameStatus.ONGOING,
                startTime
        ));

        Game game = new Game(
            gameEntity.getId(),
            whitePlayer.getId(),
            whitePlayer.getUsername(),
            blackPlayer.getId(),
            blackPlayer.getUsername(),
            gameEntity.getWhite().getElo(timeControl),
            gameEntity.getBlack().getElo(timeControl),
            timeControl,
            timeControl.initialMs(),
            timeControl.initialMs(),
            startTime.toEpochMilli()
        );

        games.put(game.getId(), game);

        scheduleFlagCheck(game, true);

        eventPublisher.publishEvent(new MatchStartedEvent(
            game.getId(),
            whitePlayer.getId(),
            whitePlayer.getUsername(),
            blackPlayer.getId(),
            blackPlayer.getUsername(),
            gameEntity.getWhite().getElo(timeControl),
            gameEntity.getBlack().getElo(timeControl)
        ));

        logger.info("Game started: {}. White: {}. Black: {}", game.getId(), whitePlayer.getUsername(), blackPlayer.getUsername());
    }

    @Transactional
    public void beginBotGame(UserEntity player, BotDefinition bot) {
        boolean playerIsWhite = ThreadLocalRandom.current().nextBoolean();

        UUID whiteId = playerIsWhite ? player.getId() : bot.id();
        String whiteUsername = playerIsWhite ? player.getUsername() : bot.username();
        UUID blackId = playerIsWhite ? bot.id() : player.getId();
        String blackUsername = playerIsWhite ? bot.username() : player.getUsername();

        Game game = new Game(
            UUID.randomUUID(),
            whiteId,
            whiteUsername,
            blackId,
            blackUsername,
            true
        );

        games.put(game.getId(), game);

        eventPublisher.publishEvent(new MatchStartedEvent(
                game.getId(), whiteId, whiteUsername, blackId, blackUsername, 0, 0
        ));

        logger.info("Bot game started: {}. White: {}. Black: {}", game.getId(), whiteUsername, blackUsername);
    }

    @Transactional
    public void makeMove(UUID userId, WsMoveDTO moveDTO) {
        Game game = getGame(moveDTO.gameId()).orElseThrow(GameNotFoundException::new);

        ReentrantLock lock = game.lockGame();
        lock.lock();

        try {
            if (!isUserTurn(game, userId)) return;

            boolean isWhite = game.getWhiteId().equals(userId);
            if (!game.isBotGame() && handleTime(game, isWhite)) {
                GameStatus flagStatus = isWhite ? GameStatus.BLACK_WIN : GameStatus.WHITE_WIN;
                handleGameEnd(game, flagStatus);
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
                    scheduleFlagCheck(game, !isWhite);
                }

                eventPublisher.publishEvent(new MoveMadeEvent(
                        game.getId(), game.getWhiteId(), game.getBlackId(), moveDTO.move(), game.isWhiteTurn(), game.getTimeControl().incrementMs()
                ));
            } else if (gameStatus != null) {
                game.addMove(moveDTO.move());

                if (game.isBotGame()) handleBotGameEnd(game, gameStatus);
                else handleGameEnd(game, gameStatus);
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

    public GameStateDTO restoreGameState() {
        JwtPrincipal jwtPrincipal = JwtService.getCurrentUser();
        if (jwtPrincipal == null) throw new InvalidUserException();

        UUID userId = jwtPrincipal.userId();

        GameStateDTO gameNullable = games.values().stream()
                .filter(g -> g.getWhiteId().equals(userId) || g.getBlackId().equals(userId))
                .findFirst()
                .map(game -> {
                    long elapsed = System.currentTimeMillis() - game.getTurnStartTime();
                    int whiteRemaining = game.isWhiteTurn()
                            ? Math.max(0, game.getWhiteRemainingMs() - (int) elapsed)
                            : game.getWhiteRemainingMs();
                    int blackRemaining = game.isWhiteTurn()
                            ? game.getBlackRemainingMs()
                            : Math.max(0, game.getBlackRemainingMs() - (int) elapsed);

                    return new GameStateDTO(
                        game.getId(),
                        game.getWhiteId(),
                        game.getWhiteUsername(),
                        game.getBlackId(),
                        game.getBlackUsername(),
                        game.getMoves(),
                        game.isWhiteDraw(),
                        game.isBlackDraw(),
                        game.getWhiteElo(),
                        game.getBlackElo(),
                        whiteRemaining,
                        blackRemaining
                    );
                })
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
            game.setEndedBy(EndedBy.RESIGNATION);
            if (game.isBotGame()) handleBotGameEnd(game, gameStatus);
            else handleGameEnd(game, gameStatus);
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
                game.setEndedBy(EndedBy.AGREEMENT);
                if (game.isBotGame()) handleBotGameEnd(game, GameStatus.DRAW);
                else handleGameEnd(game, GameStatus.DRAW);
            } else {
                UUID otherUser = isWhite ? game.getBlackId() : game.getWhiteId();
                notificationService.sendDrawOffer(game.getId(), otherUser);
            }
        } finally {
            lock.unlock();
        }
    }

    private void handleBotGameEnd(Game game, GameStatus gameStatus) {
        if (!game.getMoves().isEmpty()) {
            eventPublisher.publishEvent(new MoveMadeEvent(
                    game.getId(), game.getWhiteId(), game.getBlackId(), game.getMoves().getLast(), game.isWhiteTurn(), 0
            ));
        }

        eventPublisher.publishEvent(new MatchEndedEvent(
                game.getId(), game.getWhiteId(), game.getBlackId(), gameStatus, game.getEndedBy(),
                game.getWhiteElo(), game.getBlackElo()
        ));
        games.remove(game.getId());

        logger.info("Bot game ended: {}. White: {}. Black: {}. Result: {}", game.getId(), game.getWhiteUsername(), game.getBlackUsername(), gameStatus.name());
    }

    private void handleGameEnd(Game game, GameStatus gameStatus) {
        game.setStatus(gameStatus);
        String moves = PgnConverter.toPgn(game);

        gameRepository.findById(game.getId()).ifPresent(entity -> {
            entity.setMoves(moves);
            entity.setStatus(gameStatus);
            gameRepository.save(entity);
        });

        if (!game.getMoves().isEmpty()) {
            eventPublisher.publishEvent(new MoveMadeEvent(
                    game.getId(), game.getWhiteId(), game.getBlackId(), game.getMoves().getLast(), game.isWhiteTurn(), game.getTimeControl().initialMs()
            ));
        }

        int[] newElo = userService.updateUserElo(game.getTimeControl(), game.getWhiteId(), game.getBlackId(), gameStatus);

        eventPublisher.publishEvent(new MatchEndedEvent(game.getId(), game.getWhiteId(), game.getBlackId(), gameStatus, game.getEndedBy(), newElo[0], newElo[1]));
        games.remove(game.getId());
        flagTasks.remove(game.getId());

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

    private boolean handleTime(Game game, boolean isWhite){
        long now = System.currentTimeMillis();
        long elapsed = now - game.getTurnStartTime();

        int remaining = (isWhite ? game.getWhiteRemainingMs() : game.getBlackRemainingMs()) - (int) elapsed;

        if (isWhite) game.setWhiteRemainingMs(remaining);
        else game.setBlackRemainingMs(remaining);

        if (remaining <= 0){
            game.setEndedBy(EndedBy.TIMEOUT);
            return true;
        }

        return false;
    }

    private void scheduleFlagCheck(Game game, boolean isWhite){
        ScheduledFuture<?> prev = flagTasks.remove(game.getId());
        if (prev != null) prev.cancel(false);

        long remainingMs = isWhite ? game.getWhiteRemainingMs() : game.getBlackRemainingMs();

        ScheduledFuture<?> task = scheduler.schedule(() -> {
            ReentrantLock lock = game.lockGame();
            lock.lock();

            try {
                if (game.getStatus() != GameStatus.ONGOING) return;
                game.setEndedBy(EndedBy.TIMEOUT);
                GameStatus result = isWhite ? GameStatus.BLACK_WIN : GameStatus.WHITE_WIN;
                handleGameEnd(game, result);
            } finally {
                lock.unlock();
            }
        }, remainingMs, TimeUnit.MILLISECONDS);

        flagTasks.put(game.getId(), task);
    }
}
