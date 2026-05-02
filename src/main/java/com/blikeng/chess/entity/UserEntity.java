package com.blikeng.chess.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
public class UserEntity {
    public UserEntity(String username, String password) {
        this.username = username;
        this.password = password;
    }

    protected UserEntity() {}

    @Id
    private final UUID id = UUID.randomUUID();

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    private String bio;

    private String email;

    private String avatarUrl;

    @Setter
    private int elo = 800;
}
