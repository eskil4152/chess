package com.blikeng.chess.dto.websocket;

import java.util.UUID;

public record WsGameStartedDTO(
        WsMessageType type,
        UUID gameId,
        UUID whiteId,
        String whiteUsername,
        UUID blackId,
        String blackUsername
) {
    public WsGameStartedDTO(UUID gameId, UUID whiteId, String whiteUsername, UUID blackId, String blackUsername) {
        this(WsMessageType.GAME_STARTED, gameId, whiteId, whiteUsername, blackId, blackUsername);
    }
}
