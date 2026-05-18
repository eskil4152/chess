package com.blikeng.chess.dto.websocket;

public record WsChallengeExpired(
    WsMessageType type
) {
    public WsChallengeExpired() {
        this(WsMessageType.CHALLENGE_EXPIRED);
    }
}
