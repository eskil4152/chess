package com.blikeng.chess.events;

import java.util.UUID;

public record MatchStartedEvent(
        UUID gameId,
        UUID whiteId,
        String whiteUsername,
        UUID blackId,
        String blackUsername,
        int whiteElo,
        int blackElo
) {
}