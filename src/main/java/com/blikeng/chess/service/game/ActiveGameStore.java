package com.blikeng.chess.service.game;

import com.blikeng.chess.exception.types.InvalidUUIDException;
import com.blikeng.chess.model.Game;
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
    private final ConcurrentHashMap<UUID, Game> games = new ConcurrentHashMap<>();

    public void add(Game game) {
        games.put(game.getId(), game);
    }

    public void remove(UUID gameId) {
        games.remove(gameId);
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
        return findByUser(userId).isPresent();
    }
}
