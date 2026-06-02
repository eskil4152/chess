package com.blikeng.chess.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;

import java.util.UUID;

@Entity
@Getter
public class FriendRequestEntity {
    protected FriendRequestEntity() {}

    public FriendRequestEntity(UUID fromUser, UUID toUser) {
        this.fromUser = fromUser;
        this.toUser = toUser;
    }

    @Id
    UUID id = UUID.randomUUID();

    UUID fromUser;
    UUID toUser;
}
