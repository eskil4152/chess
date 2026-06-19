package com.blikeng.chess.dto;

/** A single profile-field edit: the field name and its new value. */
public record ProfileEditDTO(
    String field,
    String newValue
) {
}
