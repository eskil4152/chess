package com.blikeng.chess.dto.websocket;

public enum WsMessageType {
    GAME_STARTED,
    GAME_STATE,
    MOVE,
    RESIGN,
    DRAW,
    GAME_ENDED
}
