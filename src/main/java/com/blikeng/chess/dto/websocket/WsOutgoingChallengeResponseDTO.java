package com.blikeng.chess.dto.websocket;

import java.util.UUID;

public record WsOutgoingChallengeResponseDTO(
    UUID challengeId,
    String receiver
) {
}
