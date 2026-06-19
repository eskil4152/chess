package com.blikeng.chess.dto;

/** A reply to a friend request: the request id and whether it was accepted. */
public record FriendRequestResponseDTO (
    String id,
    boolean accepted
) {
}
