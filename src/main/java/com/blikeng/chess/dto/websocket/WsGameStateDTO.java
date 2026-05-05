package com.blikeng.chess.dto.websocket;

import java.util.List;
import java.util.UUID;

public record WsGameStateDTO(
        WsMessageType type,
        UUID gameId,
        UUID whiteId,
        String whiteUsername,
        UUID blackId,
        String blackUsername,
        List<String> moves,
        boolean whiteDrawOffer,
        boolean blackDrawOffer
) {
    public WsGameStateDTO(UUID gameId, UUID whiteId, String whiteUsername, UUID blackId, String blackUsername, List<String> moves, boolean whiteDrawOffer, boolean blackDrawOffer) {
        this(WsMessageType.GAME_STATE, gameId, whiteId, whiteUsername, blackId, blackUsername, moves, whiteDrawOffer, blackDrawOffer);
    }
}