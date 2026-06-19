package com.blikeng.chess.dto.websocket;

import java.util.UUID;

/** Outbound FRIEND_REQUEST notification to the recipient. */
public record WsFriendRequestDTO (
    String type,
    UUID requestId,
    String fromUsername
){
    public WsFriendRequestDTO(UUID requestId, String fromUsername){
        this("FRIEND_REQUEST", requestId, fromUsername);
    }
}
