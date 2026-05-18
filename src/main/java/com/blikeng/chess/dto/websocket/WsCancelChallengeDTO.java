package com.blikeng.chess.dto.websocket;

import java.util.UUID;

public record WsCancelChallengeDTO(
    UUID challengeId
) {
}
