package com.blikeng.chess.service.game;

import com.blikeng.chess.model.EndedBy;
import com.blikeng.chess.model.Game;
import com.blikeng.chess.model.GameStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

@Service
public class GameClockService {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final ConcurrentHashMap<UUID, ScheduledFuture<?>> flagTasks = new ConcurrentHashMap<>();

    public boolean handleTime(Game game, boolean isWhite){
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

    public void scheduleFlagCheck(Game game, boolean isWhite, BiConsumer<Game, GameStatus> onFlag){
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
                onFlag.accept(game, result);
            } finally {
                lock.unlock();
            }
        }, remainingMs, TimeUnit.MILLISECONDS);

        flagTasks.put(game.getId(), task);
    }

    public void cancel(UUID gameId){
        ScheduledFuture<?> task = flagTasks.remove(gameId);
        if (task != null) task.cancel(false);
    }
}
