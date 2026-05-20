package com.blikeng.chess.dto.websocket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WsMoveDTO(
        WsMessageType type,
        String gameId,
        String move,
        Integer increment,
        Boolean whiteMove
) {
    public WsMoveDTO(String gameId, String move, Integer increment, Boolean whiteMove) {
        this(WsMessageType.MOVE, gameId, move, increment, whiteMove);
    }
}
