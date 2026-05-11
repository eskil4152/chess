package com.blikeng.chess.dto;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public record FriendDTO(
    UUID userId,
    String username,
    String bio,
    String avatarUrl
) {

}
