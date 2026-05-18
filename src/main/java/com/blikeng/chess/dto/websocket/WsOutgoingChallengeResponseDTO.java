package com.blikeng.chess.dto.websocket;

import java.util.UUID;

public record WsOutgoingChallengeResponseDTO(
    WsMessageType type,
    UUID challengeId,
    String receiver
) {
    public WsOutgoingChallengeResponseDTO(UUID challengeId, String receiver){
        this(WsMessageType.CHALLENGE_DECLINED, challengeId, receiver);
    }
}
