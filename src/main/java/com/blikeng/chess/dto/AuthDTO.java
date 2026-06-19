package com.blikeng.chess.dto;

import com.blikeng.chess.security.UserRole;

import java.util.UUID;

/** Public account info (id, username, role) returned after authentication. */
public record AuthDTO(
        UUID userId,
        String username,
        UserRole role
) {
}
