package com.blikeng.chess.dto;

import com.blikeng.chess.model.GameStatus;

import java.util.UUID;

/** A finished-game summary for history lists (players, status, time control). */
public record GamePreviewDTO(
    UUID gameId,
    String blackUsername,
    String whiteUsername,
    GameStatus status,
    String timeControl
) {
}
