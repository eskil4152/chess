package com.blikeng.chess.dto;

public record LoginDTO(
        String username,
        String password,
        Boolean rememberMe
) {
    public LoginDTO {
        if (rememberMe == null) rememberMe = false;
    }
}
