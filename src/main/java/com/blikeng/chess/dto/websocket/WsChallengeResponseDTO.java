package com.blikeng.chess.dto.websocket;

import java.util.UUID;

/** Inbound CHALLENGE_RESPONSE message: accept or decline a challenge. */
public record WsChallengeResponseDTO(
    WsMessageType type,
    UUID challengeId,
    boolean accepted
) {
}
