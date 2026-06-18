package com.blikeng.chess.dto;

/** Password-change request: current and new password. */
public record PasswordDTO(
    String oldPassword,
    String newPassword
) {
}
