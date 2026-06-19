package com.blikeng.chess.dto.websocket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** MOVE message: a move (UCI) in a game, with clock increment (ms) and whose turn follows. Inbound from a player and re-broadcast to opponents/spectators. */
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
