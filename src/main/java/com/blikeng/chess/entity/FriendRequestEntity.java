package com.blikeng.chess.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.UUID;

@Entity
@Getter
public class FriendRequestEntity {
    protected FriendRequestEntity() {}

    public FriendRequestEntity(UserEntity fromUser, UUID toUser) {
        this.fromUser = fromUser;
        this.toUser = toUser;
    }

    @Id
    UUID id = UUID.randomUUID();

    @ManyToOne
    @JoinColumn(name = "from_user")
    UserEntity fromUser;

    UUID toUser;
}
