package com.blikeng.chess.events;

import java.util.Set;
import java.util.UUID;

/**
 * Published after a move is made, to update both players and any spectators.
 *
 * <p>{@code whiteTurn} is the side to move <em>after</em> this move (not who moved), and
 * {@code increment} is in milliseconds.
 */
public record MoveMadeEvent(
        UUID gameId,
        UUID whiteId,
        UUID blackId,
        String move,
        boolean whiteTurn,
        int increment,
        Set<UUID> spectators
) {
}
