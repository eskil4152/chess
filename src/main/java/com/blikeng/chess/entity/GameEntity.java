package com.blikeng.chess.entity;

import com.blikeng.chess.model.GameStatus;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
public class GameEntity {
    protected GameEntity() {}
    public GameEntity(
            UUID id,
            UserEntity white,
            UserEntity black,
            GameStatus status,
            Instant createdAt
    ){
        this.id = id;
        this.white = white;
        this.black = black;
        this.status = status;
        this.createdAt = createdAt;
    }

    @Id
    UUID id;

    @ManyToOne
    private UserEntity white;

    @ManyToOne
    private UserEntity black;

    @Setter
    private GameStatus status;

    private Instant createdAt;

    @Setter
    @ElementCollection
    private List<String> moves;
}
