package com.blikeng.chess.notifications.events;

import java.util.UUID;

public record ChallengeDeclinedEvent(
    UUID challengeId,
    UUID challengerId,
    String challengedUsername
) {
}
