package com.blikeng.chess.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;

import java.util.UUID;

@Entity
@Getter
public class FriendRequestEntity {
    @Id
    UUID id = UUID.randomUUID();

    UUID fromUser;
    UUID toUser;
}
