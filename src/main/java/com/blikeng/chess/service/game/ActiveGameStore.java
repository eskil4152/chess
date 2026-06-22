package com.blikeng.chess.service.game;

import com.blikeng.chess.exception.types.InvalidUUIDException;
import com.blikeng.chess.model.Game;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of live games, keyed by game id.
 *
 * <p>Owns the active-game map and the lookups over it; persistence (finished games) and
 * history live elsewhere. Per-game concurrency is handled by each {@link Game}'s own lock,
 * not here.
 */
@Service
public class ActiveGameStore {
    private final RedisTemplate<String, String> redisTemplate;

    public ActiveGameStore(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private final ConcurrentHashMap<UUID, Game> games = new ConcurrentHashMap<>();

    public void add(Game game) {
        games.put(game.getId(), game);

        redisTemplate.opsForValue().set("game:user:" + game.getWhiteId(), game.getId().toString());
        redisTemplate.opsForValue().set("game:user:" + game.getBlackId(), game.getId().toString());
    }

    public void remove(UUID gameId) {
        Game game = games.remove(gameId);
        if (game == null) return;

        redisTemplate.delete("game:user:" + game.getWhiteId());
        redisTemplate.delete("game:user:" + game.getBlackId());
    }

    public Optional<Game> get(UUID gameId) {
        return Optional.ofNullable(games.get(gameId));
    }

    /** Looks up a game by its string id, throwing {@link InvalidUUIDException} if malformed. */
    public Optional<Game> get(String gameIdString) {
        try {
            return Optional.ofNullable(games.get(UUID.fromString(gameIdString)));
        } catch (IllegalArgumentException _) {
            throw new InvalidUUIDException();
        }
    }

    /** The active game a user is a player in, if any. */
    public Optional<Game> findByUser(UUID userId) {
        return games.values().stream()
            .filter(g -> g.getWhiteId().equals(userId) || g.getBlackId().equals(userId))
            .findFirst();
    }

    public boolean isInGame(UUID userId) {
        return redisTemplate.opsForValue().get("game:user:" + userId) != null;
    }
}
