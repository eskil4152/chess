package com.blikeng.chess.dto.websocket;

public record WsResignDTO(
        WsMessageType type,
        String gameId
) {
}