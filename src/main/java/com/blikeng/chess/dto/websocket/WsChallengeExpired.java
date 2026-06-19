package com.blikeng.chess.dto.websocket;

/** Outbound CHALLENGE_EXPIRED notification to the challenger. */
public record WsChallengeExpired(
    WsMessageType type
) {
    public WsChallengeExpired() {
        this(WsMessageType.CHALLENGE_EXPIRED);
    }
}
