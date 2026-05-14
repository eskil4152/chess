package com.blikeng.chess.bot;

import com.blikeng.chess.dto.websocket.WsMoveDTO;
import com.blikeng.chess.engine.PositionMapper;
import com.blikeng.chess.engine.analysis.Evaluator;
import com.blikeng.chess.engine.analysis.Evaluator.MoveEval;
import com.blikeng.chess.model.piece.PieceType;
import com.blikeng.chess.notifications.events.MatchStartedEvent;
import com.blikeng.chess.notifications.events.MoveMadeEvent;
import com.blikeng.chess.service.GameService;
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

@Service
public class BotService {
    private final GameService gameService;
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

    public BotService(GameService gameService) {
        this.gameService = gameService;
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
                gameService.getActiveGame(botId).ifPresent(game -> {
                    MoveEval eval = Evaluator.getBestMove(game, bot.difficulty().depth, bot.difficulty().noise);
                    if (eval.move() == null) return;

                    String uci = PositionMapper.toString(eval.move().from())
                               + PositionMapper.toString(eval.move().to());
                    if (eval.promoPiece() != null) {
                        uci += Character.toLowerCase(PieceType.toChar(eval.promoPiece()));
                    }

                    gameService.makeMove(botId, new WsMoveDTO(gameId.toString(), uci));
                });
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.warn("Bot move failed for game {}: {}", gameId, e.getMessage());
            }
        });
    }
}
