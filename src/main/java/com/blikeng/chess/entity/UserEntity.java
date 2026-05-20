package com.blikeng.chess.entity;

import com.blikeng.chess.security.UserRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "users")
public class UserEntity {
    public UserEntity(String username, String password) {
        this.username = username;
        this.password = password;
    }

    protected UserEntity() {}

    @Id
    @Setter(AccessLevel.NONE)
    private final UUID id = UUID.randomUUID();

    @Column(nullable = false, unique = true)
    @Setter(AccessLevel.NONE)
    private String username;

    @Column(nullable = false)
    private String password;

    private String bio;

    private String email;

    private String avatarUrl;

    private int bulletElo = 800;
    private boolean been2400Bullet = false;

    private int blitzElo = 800;
    private boolean been2400Blitz = false;

    private int rapidElo = 800;
    private boolean been2400Rapid = false;

    private int blitzGames = 0;
    private int rapidGames = 0;
    private int bulletGames = 0;

    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.USER;
}
