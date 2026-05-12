package com.blikeng.chess.dto;

import java.util.UUID;

public record FriendDTO(
    UUID userId,
    String username,
    String bio,
    String avatarUrl
) {

}
