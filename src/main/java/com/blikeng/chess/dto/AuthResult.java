package com.blikeng.chess.dto;

/** Internal auth result: the issued JWT plus the user's {@link AuthDTO}. */
public record AuthResult(
        String token,
        AuthDTO user
) {
}
