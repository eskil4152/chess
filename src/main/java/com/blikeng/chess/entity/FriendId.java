package com.blikeng.chess.entity;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class FriendId implements Serializable {
    private UUID userA;
    private UUID userB;

    public FriendId(UUID userA, UUID userB) {
        this.userA = userA;
        this.userB = userB;
    }
}
