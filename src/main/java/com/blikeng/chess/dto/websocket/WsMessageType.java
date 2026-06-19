package com.blikeng.chess.dto.websocket;

/** Discriminator for all WebSocket messages (the {@code type} field). */
public enum WsMessageType {
    GAME_STARTED,
    GAME_STATE,
    MOVE,
    RESIGN,
    OFFER_DRAW,
    GAME_ENDED,
    CHALLENGE,
    CANCEL_CHALLENGE,
    CHALLENGE_DECLINED,
    CHALLENGE_RESPONSE,
    CHALLENGE_CANCELLED,
    CHALLENGE_EXPIRED
}
