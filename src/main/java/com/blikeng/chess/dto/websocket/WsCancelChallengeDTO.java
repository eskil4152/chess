package com.blikeng.chess.dto.websocket;

import java.util.UUID;

/** Inbound CANCEL_CHALLENGE message: withdraw a sent challenge. */
public record WsCancelChallengeDTO(
    WsMessageType type,
    UUID challengeId
) {
}
