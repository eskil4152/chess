package com.blikeng.chess.dto.websocket;

import com.blikeng.chess.model.EndedBy;
import com.blikeng.chess.model.GameStatus;

import java.util.UUID;

/** Outbound GAME_ENDED message: result, reason, and final Elos. */
public record WsGameEndedDTO(
        WsMessageType type,
        UUID gameId,
        GameStatus status,
        EndedBy endedBy,
        int whiteElo,
        int blackElo
) {
    public WsGameEndedDTO(UUID gameId, GameStatus status, EndedBy endedBy, int whiteElo, int blackElo) {
        this(WsMessageType.GAME_ENDED, gameId, status, endedBy, whiteElo, blackElo);
    }
}
