package com.blikeng.chess.notifications.events;

import java.util.UUID;

public record ChallengeEvent(
    UUID challengeId,
    UUID senderId,
    String senderUsername,
    UUID receiverId,
    String timeControl
) {
}
