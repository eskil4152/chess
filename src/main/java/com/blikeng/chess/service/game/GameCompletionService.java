package com.blikeng.chess.service.game;

import com.blikeng.chess.engine.converter.PgnConverter;
import com.blikeng.chess.events.MatchEndedEvent;
import com.blikeng.chess.events.MoveMadeEvent;
import com.blikeng.chess.model.Game;
import com.blikeng.chess.model.GameStatus;
import com.blikeng.chess.repository.GameRepository;
import com.blikeng.chess.service.StatsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class GameCompletionService {
    private final Logger logger = LoggerFactory.getLogger(GameCompletionService.class);

    private final ApplicationEventPublisher eventPublisher;
    private final GameRepository gameRepository;
    private final StatsService statsService;
    private final ActiveGameStore activeGameStore;
    private final GameClockService gameClockService;

    public GameCompletionService(
        ApplicationEventPublisher eventPublisher,
        GameRepository gameRepository,
        StatsService statsService,
        ActiveGameStore activeGameStore,
        GameClockService gameClockService
    ){
        this.eventPublisher = eventPublisher;
        this.gameRepository = gameRepository;
        this.statsService = statsService;
        this.activeGameStore = activeGameStore;
        this.gameClockService = gameClockService;
    }

    public void endGame(Game game, GameStatus gameStatus) {
        if (game.isBotGame()) handleBotGameEnd(game, gameStatus);
        else handleGameEnd(game, gameStatus);

        activeGameStore.remove(game.getId());
        gameClockService.cancel(game.getId());
    }

    private void handleBotGameEnd(Game game, GameStatus gameStatus) {
        if (!game.getMoves().isEmpty()) {
            eventPublisher.publishEvent(new MoveMadeEvent(
                game.getId(), game.getWhiteId(), game.getBlackId(), game.getMoves().getLast(), game.isWhiteTurn(), 0, game.getSpectators()
            ));
        }

        eventPublisher.publishEvent(new MatchEndedEvent(
            game.getId(), game.getWhiteId(), game.getBlackId(), gameStatus, game.getEndedBy(),
            game.getWhiteElo(), game.getBlackElo(), game.getSpectators()
        ));

        logger.info("Bot game ended: {}. White: {}. Black: {}. Result: {}", game.getId(), game.getWhiteUsername(), game.getBlackUsername(), gameStatus.name());
    }

    private void handleGameEnd(Game game, GameStatus gameStatus) {
        game.setStatus(gameStatus);
        String moves = PgnConverter.toPgn(game);

        gameRepository.findById(game.getId()).ifPresent(entity -> {
            entity.setMoves(moves);
            entity.setStatus(gameStatus);
            entity.setEndedBy(game.getEndedBy());
            gameRepository.save(entity);
        });

        if (!game.getMoves().isEmpty()) {
            eventPublisher.publishEvent(new MoveMadeEvent(
                game.getId(), game.getWhiteId(), game.getBlackId(), game.getMoves().getLast(), game.isWhiteTurn(), game.getTimeControl().initialMs(), game.getSpectators()
            ));
        }

        int[] newElo = statsService.updateUserStatsAndReturnNewElos(game.getTimeControl(), game.getWhiteId(), game.getBlackId(), gameStatus);

        eventPublisher.publishEvent(new MatchEndedEvent(game.getId(), game.getWhiteId(), game.getBlackId(), gameStatus, game.getEndedBy(), newElo[0], newElo[1], game.getSpectators()));

        logger.info("Game ended: {}. Black: {}. White: {}. Result: {}", game.getId(), game.getWhiteUsername(), game.getBlackUsername(), gameStatus.name());
    }
}
