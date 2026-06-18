package com.blikeng.chess.dto.websocket;

import java.util.UUID;

/** Outbound GAME_STARTED message: both players' details for a new game. */
public record WsGameStartedDTO(
        WsMessageType type,
        UUID gameId,
        UUID whiteId,
        String whiteUsername,
        UUID blackId,
        String blackUsername,
        int whiteElo,
        int blackElo
) {
    public WsGameStartedDTO(UUID gameId, UUID whiteId, String whiteUsername, UUID blackId, String blackUsername, int whiteElo, int blackElo) {
        this(WsMessageType.GAME_STARTED, gameId, whiteId, whiteUsername, blackId, blackUsername, whiteElo, blackElo);
    }
}
