package com.blikeng.chess.dto;

import java.util.UUID;

public record FriendRequest(
    UUID requestId,
    String username,
    String avatarUrl
) {
}
