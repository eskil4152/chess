package com.blikeng.chess.dto;

import java.util.UUID;

/** A friend entry for the friends list, including online status. */
public record FriendDTO(
    UUID userId,
    String username,
    String bio,
    String avatarUrl,
    boolean isOnline
) {

}
