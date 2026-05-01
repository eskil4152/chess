package com.blikeng.chess.dto.websocket;

import com.blikeng.chess.model.GameStatus;

import java.util.UUID;

public record WsGameEndedDTO(
        WsMessageType type,
        UUID gameId,
        GameStatus status
) {
    public WsGameEndedDTO(UUID gameId, GameStatus status) {
        this(WsMessageType.GAME_ENDED, gameId, status);
    }
}
