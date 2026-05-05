package com.blikeng.chess.dto.websocket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WsResignDTO(
        WsMessageType type,
        String gameId
) {
    public WsResignDTO(String gameId) {
        this(WsMessageType.RESIGN, gameId);
    }
}
