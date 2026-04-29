package com.blikeng.chess.dto;

public record GameDTO(
        String whiteUsername,
        String blackUsername,
        String whiteId,
        String blackId
) {
}
