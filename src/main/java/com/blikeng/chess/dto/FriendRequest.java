package com.blikeng.chess.dto;

import java.util.UUID;

/** A single incoming friend request (request id + sender info). */
public record FriendRequest(
    UUID requestId,
    String username,
    String avatarUrl
) {
}
