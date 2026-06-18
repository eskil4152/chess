package com.blikeng.chess.dto;

import com.blikeng.chess.dto.websocket.WsMessageType;

import java.util.List;
import java.util.UUID;

/** Full state of an active game pushed to clients, including per-side remaining time (WS GAME_STATE). */
public record GameStateDTO(
        WsMessageType type,
        UUID gameId,
        UUID whiteId,
        String whiteUsername,
        UUID blackId,
        String blackUsername,
        List<String> moves,
        boolean whiteDrawOffer,
        boolean blackDrawOffer,
        int whiteElo,
        int blackElo,
        int whiteRemainingMs,
        int blackRemainingMs
) {
    public GameStateDTO(
        UUID gameId,
        UUID whiteId,
        String whiteUsername,
        UUID blackId,
        String blackUsername,
        List<String> moves,
        boolean whiteDrawOffer,
        boolean blackDrawOffer,
        int whiteElo,
        int blackElo,
        int whiteRemainingMs,
        int blackRemainingMs
    ) {
        this(WsMessageType.GAME_STATE, gameId, whiteId, whiteUsername, blackId, blackUsername, moves, whiteDrawOffer, blackDrawOffer, whiteElo, blackElo, whiteRemainingMs, blackRemainingMs);
    }
}