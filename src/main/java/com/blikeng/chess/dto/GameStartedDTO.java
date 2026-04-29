package com.blikeng.chess.dto;

import java.util.UUID;

public record GameStartedDTO(
        UUID gameId,
        UUID whiteId,
        String whiteUsername,
        UUID blackId,
        String blackUsername
) {
}
