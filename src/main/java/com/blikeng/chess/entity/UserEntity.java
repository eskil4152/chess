package com.blikeng.chess.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.UUID;

@Entity
public class UserEntity {
    @Id
    private UUID id;

    private String username;
    private String password;

    public String getUsername() {
        return username;
    }

    public UUID getId() {
        return id;
    }
}
