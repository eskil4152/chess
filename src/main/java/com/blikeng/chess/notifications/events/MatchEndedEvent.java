package com.blikeng.chess.notifications.events;

import com.blikeng.chess.model.EndedBy;
import com.blikeng.chess.model.GameStatus;

import java.util.UUID;

public record MatchEndedEvent(
        UUID gameId,
        UUID whiteId,
        UUID blackId,
        GameStatus status,
        EndedBy endedBy,
        int whiteElo,
        int blackElo
) {
}
