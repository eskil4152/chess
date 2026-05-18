package com.blikeng.chess.model;

import com.blikeng.chess.model.timecontrol.TimeControl;

import java.time.Instant;
import java.util.UUID;

public record Challenge(
    UUID id,
    UUID challengerId,
    UUID challengedId,
    TimeControl timeControl,
    Instant sent
) {
}
