package com.blikeng.chess.events;

import java.util.UUID;

/** Published after a game starts, so both players can be notified and a bot can open. */
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