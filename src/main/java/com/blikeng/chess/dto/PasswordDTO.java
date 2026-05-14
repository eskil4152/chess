package com.blikeng.chess.dto;

public record PasswordDTO(
    String oldPassword,
    String newPassword
) {
}
