package com.blikeng.chess.entity;

import com.blikeng.chess.model.GameStatus;
import jakarta.persistence.*;
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
    private UserEntity white;

    @ManyToOne
    private UserEntity black;

    @Setter
    @Enumerated(EnumType.STRING)
    private GameStatus status;

    private Instant createdAt;

    @Setter
    @ElementCollection
    @CollectionTable(name = "game_moves", joinColumns = @JoinColumn(name = "game_id"))
    private List<String> moves;
}
