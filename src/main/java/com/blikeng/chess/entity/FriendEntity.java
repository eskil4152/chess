package com.blikeng.chess.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;

@Entity
@Table(name = "friends")
@Getter
public class FriendEntity {
    protected FriendEntity() {}

    public FriendEntity(FriendId id, UserEntity userA, UserEntity userB) {
        this.id = id;
        this.userA = userA;
        this.userB = userB;
    }

    @EmbeddedId
    private FriendId id;

    @ManyToOne
    @MapsId("userA")
    @JoinColumn(name = "user_a")
    private UserEntity userA;

    @ManyToOne
    @MapsId("userB")
    @JoinColumn(name = "user_b")
    private UserEntity userB;

    private Instant friendsSince = Instant.now();
}
