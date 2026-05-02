package com.blikeng.chess.service;

import com.blikeng.chess.entity.UserEntity;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MatchmakingService {
    private final AuthService authService;
    private final GameService gameService;

    public MatchmakingService(AuthService authService, GameService gameService) {
        this.authService = authService;
        this.gameService = gameService;
    }

    private final ConcurrentHashMap<UUID, UserEntity> queue = new ConcurrentHashMap<>();

    public void queuePlayer(UUID userId) {
        UserEntity user = authService.findUserById(userId).orElseThrow();

        UserEntity matched;

        synchronized (queue) {
            if (queue.containsKey(userId)) return;

            var best = queue.entrySet().stream()
                    .min(Comparator.comparingInt(e -> Math.abs(e.getValue().getElo() - user.getElo())))
                    .filter(e -> Math.abs(e.getValue().getElo() - user.getElo()) <= 200)
                    .orElse(null);

            if (best == null) {
                queue.put(userId, user);
                return;
            }

            queue.remove(best.getKey());
            matched = best.getValue();

            gameService.beginGame(matched, user);
        }
    }

    public void dequeuePlayer(UUID userId) {
        queue.remove(userId);
    }
}
