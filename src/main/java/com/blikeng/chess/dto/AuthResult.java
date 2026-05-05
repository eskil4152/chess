package com.blikeng.chess.dto;

public record AuthResult(
        String token,
        AuthDTO user
) {
}
