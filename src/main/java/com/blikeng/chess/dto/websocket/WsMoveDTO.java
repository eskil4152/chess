package com.blikeng.chess.dto.websocket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WsMoveDTO(
        WsMessageType type,
        String gameId,
        String move
) {
    public WsMoveDTO(String gameId, String move) {
        this(WsMessageType.MOVE, gameId, move);
    }
}
