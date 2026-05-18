package com.blikeng.chess.dto.websocket;

import java.util.UUID;

public record WsChallengeResponseDTO(
    UUID challengeId,
    boolean accepted
) {
}
