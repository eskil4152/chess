package com.blikeng.chess.dto.websocket;

import java.util.UUID;

public record WsCancelChallengeDTO(
    WsMessageType type,
    UUID challengeId
) {
}
