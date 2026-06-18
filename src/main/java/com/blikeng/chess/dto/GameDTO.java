package com.blikeng.chess.dto;

import com.blikeng.chess.model.GameStatus;

import java.util.UUID;

/** A finished game's full detail: players, status, and the move list (PGN). */
public record GameDTO(
        UUID gameId,
        String blackUsername,
        String whiteUsername,
        GameStatus status,
        String moves
) {
}
