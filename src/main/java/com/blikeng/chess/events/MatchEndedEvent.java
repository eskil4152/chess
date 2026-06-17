package com.blikeng.chess.events;

import com.blikeng.chess.model.EndedBy;
import com.blikeng.chess.model.GameStatus;

import java.util.Set;
import java.util.UUID;

/** Published after a game ends, carrying the result and final Elos for both sides. */
public record MatchEndedEvent(
        UUID gameId,
        UUID whiteId,
        UUID blackId,
        GameStatus status,
        EndedBy endedBy,
        int whiteElo,
        int blackElo,
        Set<UUID> spectators
) {
}
