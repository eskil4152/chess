package com.blikeng.chess.notifications.events;

import java.util.UUID;

public record ChallengeResponseEvent(
    UUID challengeId,
    boolean accepted
) {
}
