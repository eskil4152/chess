package com.blikeng.chess.events;

import com.blikeng.chess.entity.UserEntity;

import java.util.UUID;

public record FriendRequestEvent(
    UUID id,
    UserEntity fromUser,
    UserEntity toUser
){
}
