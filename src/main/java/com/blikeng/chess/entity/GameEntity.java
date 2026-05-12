package com.blikeng.chess.entity;

import com.blikeng.chess.model.GameStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Table(name = "games")
public class GameEntity {
    protected GameEntity() {}
    public GameEntity(
            UserEntity white,
            UserEntity black,
            GameStatus status,
            Instant createdAt
    ){
        this.white = white;
        this.black = black;
        this.status = status;
        this.createdAt = createdAt;
    }

    @Id
    private final UUID id = UUID.randomUUID();

    @ManyToOne
    @JoinColumn(name = "white")
    private UserEntity white;

    @ManyToOne
    @JoinColumn(name = "black")
    private UserEntity black;

    @Setter
    @Enumerated(EnumType.STRING)
    private GameStatus status;

    private Instant createdAt;

    @Setter
    private String moves = "";
}
