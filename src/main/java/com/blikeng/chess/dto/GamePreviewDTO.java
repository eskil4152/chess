package com.blikeng.chess.dto;

import com.blikeng.chess.model.GameStatus;

import java.util.UUID;

public record GamePreviewDTO(
        UUID gameId,
        String blackUsername,
        String whiteUsername,
        GameStatus status
) {
}
