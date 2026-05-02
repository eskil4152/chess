package com.blikeng.chess.dto;

import com.blikeng.chess.model.GameStatus;

import java.util.List;
import java.util.UUID;

public record GameDTO(
        UUID gameId,
        String blackUsername,
        String whiteUsername,
        GameStatus status,
        List<String> moves
) {
}
