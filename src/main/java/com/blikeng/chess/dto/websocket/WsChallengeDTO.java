package com.blikeng.chess.dto.websocket;

import java.util.UUID;

public record WsChallengeDTO(
    WsMessageType type,
    UUID receiver,
    String timeControl
) {
    public WsChallengeDTO(UUID receiver, String timeControl) {
        this(WsMessageType.CHALLENGE, receiver, timeControl);
    }
}