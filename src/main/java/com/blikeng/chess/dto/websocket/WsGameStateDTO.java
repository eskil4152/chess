package com.blikeng.chess.dto.websocket;

import java.util.List;
import java.util.UUID;

public record WsGameStateDTO(
        WsMessageType type,
        UUID gameId,
        UUID whiteId,
        String whiteUsername,
        UUID blackId,
        String blackUsername,
        List<String> moves
) {
    public WsGameStateDTO(UUID gameId, UUID whiteId, String whiteUsername, UUID blackId, String blackUsername, List<String> moves) {
        this(WsMessageType.GAME_STARTED, gameId, whiteId, whiteUsername, blackId, blackUsername, moves);
    }
}