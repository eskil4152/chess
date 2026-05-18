package com.blikeng.chess.dto.websocket;

import java.util.UUID;

public record WsOutgoingChallengeCancelledDTO(
    WsMessageType type,
    UUID challengeId,
    String challenger
) {
    public WsOutgoingChallengeCancelledDTO(UUID challengeId, String challenger) {
        this(WsMessageType.CHALLENGE_CANCELLED, challengeId, challenger);
    }
}
