package com.blikeng.chess.dto.websocket;

public record WsChallengeDTO(
    WsMessageType type,
    String sender, String
    timeControl
) {
    public WsChallengeDTO(String sender, String timeControl) {
        this(WsMessageType.CHALLENGE, sender, timeControl);
    }
}