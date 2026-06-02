package com.blikeng.chess.dto.websocket;

import java.util.UUID;

public record WsFriendRequestDTO (
    UUID requestId,
    String fromUsername
){
}
