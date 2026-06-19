package com.blikeng.chess.service.game;

import com.blikeng.chess.bot.BotDefinition;
import com.blikeng.chess.entity.GameEntity;
import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.events.MatchStartedEvent;
import com.blikeng.chess.exception.types.ExistingGameException;
import com.blikeng.chess.model.Game;
import com.blikeng.chess.model.GameStatus;
import com.blikeng.chess.model.timecontrol.TimeControl;
import com.blikeng.chess.repository.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Starts new games: human matches ({@link #beginGame} — persisted, random colours, clock
 * started) and bot games ({@link #beginBotGame} — not persisted, no clock). Both register
 * the game in {@link ActiveGameStore} and publish a {@code MatchStartedEvent}.
 */
@Service
public class GameCreationService {
    private final Logger logger = LoggerFactory.getLogger(GameCreationService.class);

    private final GameRepository gameRepository;
    private final ActiveGameStore activeGameStore;
    private final GameClockService gameClockService;
    private final ApplicationEventPublisher eventPublisher;
    private final GameCompletionService gameCompletionService;

    public GameCreationService(
        GameRepository gameRepository,
        ActiveGameStore activeGameStore,
        GameClockService gameClockService,
        ApplicationEventPublisher eventPublisher,
        GameCompletionService gameCompletionService
    ) {
        this.gameRepository = gameRepository;
        this.activeGameStore = activeGameStore;
        this.gameClockService = gameClockService;
        this.eventPublisher = eventPublisher;
        this.gameCompletionService = gameCompletionService;
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
            startTime,
            timeControl.name(),
            null
        ));

        Game game = new Game(
            gameEntity.getId(),
            whitePlayer.getId(),
            whitePlayer.getUsername(),
            blackPlayer.getId(),
            blackPlayer.getUsername(),
            gameEntity.getWhite().getElo(timeControl.type()),
            gameEntity.getBlack().getElo(timeControl.type()),
            timeControl,
            timeControl.initialMs(),
            timeControl.initialMs(),
            startTime.toEpochMilli()
        );

        activeGameStore.add(game);

        gameClockService.scheduleFlagCheck(game, true, gameCompletionService::endGame);

        eventPublisher.publishEvent(new MatchStartedEvent(
            game.getId(),
            whitePlayer.getId(),
            whitePlayer.getUsername(),
            blackPlayer.getId(),
            blackPlayer.getUsername(),
            gameEntity.getWhite().getElo(timeControl.type()),
            gameEntity.getBlack().getElo(timeControl.type())
        ));

        logger.info("Game started: {}. White: {}. Black: {}", game.getId(), whitePlayer.getUsername(), blackPlayer.getUsername());
    }

    @Transactional
    public synchronized void beginBotGame(UserEntity player, BotDefinition bot) {
        if (activeGameStore.isInGame(player.getId())) throw new ExistingGameException();

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

        activeGameStore.add(game);

        eventPublisher.publishEvent(new MatchStartedEvent(
            game.getId(), whiteId, whiteUsername, blackId, blackUsername, 0, 0
        ));

        logger.info("Bot game started: {}. White: {}. Black: {}", game.getId(), whiteUsername, blackUsername);
    }
}
