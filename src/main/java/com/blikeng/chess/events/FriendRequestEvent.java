package com.blikeng.chess.events;

import com.blikeng.chess.entity.UserEntity;

import java.util.UUID;

/** Published when a friend request is sent, to notify the recipient. */
public record FriendRequestEvent(
    UUID id,
    UserEntity fromUser,
    UserEntity toUser
){
}
