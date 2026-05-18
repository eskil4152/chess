package com.blikeng.chess.dto.websocket;

public enum WsMessageType {
    GAME_STARTED,
    GAME_STATE,
    MOVE,
    RESIGN,
    OFFER_DRAW,
    GAME_ENDED,
    CHALLENGE,
    CHALLENGE_DECLINED,
    CHALLENGE_CANCELLED
}
