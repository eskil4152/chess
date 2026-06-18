package com.blikeng.chess.bot;

import com.blikeng.chess.dto.websocket.WsMoveDTO;
import com.blikeng.chess.engine.PositionMapper;
import com.blikeng.chess.engine.analysis.Evaluator;
import com.blikeng.chess.engine.analysis.Evaluator.MoveEval;
import com.blikeng.chess.model.piece.PieceType;
import com.blikeng.chess.events.MatchStartedEvent;
import com.blikeng.chess.events.MoveMadeEvent;
import com.blikeng.chess.service.game.ActiveGameStore;
import com.blikeng.chess.service.game.GameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Handles bot actions.
 *
 * <p>{@link #BOTS} holds manually defined bots of different levels, each with a fixed
 * UUID for easy identification.
 *
 * <p>Bot moves run on a fixed pool of 4 threads, so up to 4 bot moves can be computed
 * at once; if more than 4 bots need to move, the extra moves queue and wait for a free
 * thread instead of being canceled. Each move waits 400ms first (cosmetic, so moves
 * aren't instant), then asks the engine for a move using that bot's {@code depth} and
 * {@code noise} (see {@link BotDifficulty}).
 */
@Service
public class BotService {
    private final GameService gameService;
    private final ActiveGameStore activeGameStore;
    private final Logger logger = LoggerFactory.getLogger(BotService.class);
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public static final Map<BotDifficulty, BotDefinition> BOTS = Map.of(
        BotDifficulty.EASY,   new BotDefinition(UUID.fromString("00000000-0000-0000-0000-000000000001"), "Bot-Easy",   BotDifficulty.EASY),
        BotDifficulty.MEDIUM, new BotDefinition(UUID.fromString("00000000-0000-0000-0000-000000000002"), "Bot-Medium", BotDifficulty.MEDIUM),
        BotDifficulty.HARD,   new BotDefinition(UUID.fromString("00000000-0000-0000-0000-000000000003"), "Bot-Hard",   BotDifficulty.HARD)
    );

    private static final Set<UUID> BOT_IDS = BOTS.values().stream()
            .map(BotDefinition::id)
            .collect(Collectors.toUnmodifiableSet());

    public BotService(GameService gameService, ActiveGameStore activeGameStore) {
        this.gameService = gameService;
        this.activeGameStore = activeGameStore;
    }

    public BotDefinition getBot(BotDifficulty difficulty) {
        return BOTS.get(difficulty);
    }

    public boolean isBot(UUID userId) {
        return BOT_IDS.contains(userId);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMatchStarted(MatchStartedEvent event) {
        if (isBot(event.whiteId())) {
            scheduleBotMove(event.gameId(), event.whiteId());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMoveMade(MoveMadeEvent event) {
        UUID nextPlayer = event.whiteTurn() ? event.whiteId() : event.blackId();
        if (isBot(nextPlayer)) {
            scheduleBotMove(event.gameId(), nextPlayer);
        }
    }

    private void scheduleBotMove(UUID gameId, UUID botId) {
        BotDefinition bot = BOTS.values().stream()
                .filter(b -> b.id().equals(botId))
                .findFirst()
                .orElseThrow();

        executor.submit(() -> {
            try {
                Thread.sleep(400);
                activeGameStore.findByUser(botId).ifPresent(game -> {
                    MoveEval eval = Evaluator.getBestMove(game, bot.difficulty().depth, bot.difficulty().noise);
                    if (eval.move() == null) return;

                    String uci = PositionMapper.toString(eval.move().from())
                               + PositionMapper.toString(eval.move().to());
                    if (eval.promoPiece() != null) {
                        uci += Character.toLowerCase(PieceType.toChar(eval.promoPiece()));
                    }

                    gameService.makeMove(botId, new WsMoveDTO(gameId.toString(), uci, 0, game.isWhiteTurn()));
                });
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.warn("Bot move failed for game {}: {}", gameId, e.getMessage());
            }
        });
    }
}
