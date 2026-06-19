package com.blikeng.chess.dto.websocket;

import java.util.UUID;

/** Inbound CHALLENGE message: challenge another user at a given time control. */
public record WsChallengeDTO(
    WsMessageType type,
    UUID receiver,
    String timeControl
) {
    public WsChallengeDTO(UUID receiver, String timeControl) {
        this(WsMessageType.CHALLENGE, receiver, timeControl);
    }
}