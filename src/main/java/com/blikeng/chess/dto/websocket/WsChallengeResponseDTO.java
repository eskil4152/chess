package com.blikeng.chess.dto.websocket;

import java.util.UUID;

public record WsChallengeResponseDTO(
    WsMessageType type,
    UUID challengeId,
    boolean accepted
) {
}
