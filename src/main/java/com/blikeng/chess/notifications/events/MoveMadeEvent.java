package com.blikeng.chess.notifications.events;

import java.util.UUID;

public record MoveMadeEvent(
        UUID gameId,
        UUID whiteId,
        UUID blackId,
        String move,
        boolean whiteTurn,
        int increment
) {
}
