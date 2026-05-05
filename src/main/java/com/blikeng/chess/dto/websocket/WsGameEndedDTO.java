package com.blikeng.chess.dto.websocket;

import com.blikeng.chess.model.EndedBy;
import com.blikeng.chess.model.GameStatus;

import java.util.UUID;

public record WsGameEndedDTO(
        WsMessageType type,
        UUID gameId,
        GameStatus status,
        EndedBy endedBy
) {
    public WsGameEndedDTO(UUID gameId, GameStatus status, EndedBy endedBy) {
        this(WsMessageType.GAME_ENDED, gameId, status, endedBy);
    }
}
