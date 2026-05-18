package com.blikeng.chess.notifications.events;

import java.util.UUID;

public record ChallengeEvent(
    UUID challengeId,
    UUID challengerId,
    String challengerUsername,
    UUID challengedId,
    String timeControl
) {
}
