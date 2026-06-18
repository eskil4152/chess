package com.blikeng.chess.dto.websocket;

/** Inbound RESIGN message: the game the sender resigns. */
public record WsResignDTO(
        WsMessageType type,
        String gameId
) {
    public WsResignDTO(String gameId) {
        this(WsMessageType.RESIGN, gameId);
    }
}

