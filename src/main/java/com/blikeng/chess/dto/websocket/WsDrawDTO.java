package com.blikeng.chess.dto.websocket;

/** OFFER_DRAW message: a draw offer/acceptance for a game. */
public record WsDrawDTO(
        WsMessageType type,
        String gameId
) {
    public WsDrawDTO(String gameId) {
        this(WsMessageType.OFFER_DRAW, gameId);
    }
}
