package com.blikeng.chess.entity;

import com.blikeng.chess.security.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Table(name = "users")
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
    @Setter
    private String password;

    @Setter
    private String bio;

    private String email;

    @Column(name = "avatarurl")
    @Setter
    private String avatarUrl;

    @Setter
    private int elo = 800;

    @Setter
    private boolean been2400 = false;

    @Setter
    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.USER;

    @Setter
    private int games = 0;
}
