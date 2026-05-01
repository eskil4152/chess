package com.blikeng.chess.dto;

import com.blikeng.chess.model.GameStatus;

import java.util.UUID;

public record GameDTO(
        UUID gameId,
        String blackUsername,
        String whiteUsername,
        GameStatus status,
        String moves
) {
}
