package com.blikeng.chess.dto;

/** Login/registration credentials; {@code rememberMe} extends token and cookie lifetime. */
public record LoginDTO(
        String username,
        String password,
        Boolean rememberMe
) {
    public LoginDTO {
        if (rememberMe == null) rememberMe = false;
    }
}
