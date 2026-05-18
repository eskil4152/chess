package com.blikeng.chess.dto.websocket;

import java.util.UUID;

public record WsOutgoingChallengeDTO(
    WsMessageType type,
    UUID challengeId,
    String challenger,
    String timeControl
) {
    public WsOutgoingChallengeDTO(UUID challengeId, String challenger, String timeControl){
        this(WsMessageType.CHALLENGE, challengeId, challenger, timeControl);
    }
}
