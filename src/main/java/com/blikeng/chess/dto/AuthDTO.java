package com.blikeng.chess.dto;

import com.blikeng.chess.security.UserRole;

import java.util.UUID;

public record AuthDTO(
        UUID userId,
        String username,
        UserRole role
) {
}
