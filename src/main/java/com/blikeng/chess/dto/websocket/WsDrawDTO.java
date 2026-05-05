package com.blikeng.chess.dto.websocket;

public record WsDrawDTO(
        WsMessageType type,
        String gameId
) {
    public WsDrawDTO(String gameId) {
        this(WsMessageType.OFFER_DRAW, gameId);
    }
}
