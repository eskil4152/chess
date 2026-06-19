package com.blikeng.chess.model;

import com.blikeng.chess.model.timecontrol.TimeControl;

import java.time.Instant;
import java.util.UUID;

/**
 * A pending direct challenge from one player to another at a given {@link TimeControl}.
 */
public record Challenge(
    UUID id,
    UUID challengerId,
    UUID challengedId,
    TimeControl timeControl,
    Instant sent
) {
}
