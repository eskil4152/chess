package com.blikeng.chess.dto;

import java.util.List;

/** Wrapper for the list of a user's pending friend requests. */
public record FriendRequestsDTO (
    List<FriendRequest> friendRequests
){
}
