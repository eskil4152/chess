package com.blikeng.chess.entity;

import com.blikeng.chess.model.EndedBy;
import com.blikeng.chess.model.GameStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A persisted, finished game (table {@code games}).
 *
 * <p>The move list is stored as a single {@code moves} string (PGN); {@code status} and
 * {@code endedBy} are persisted as enum names.
 */
@Entity
@Getter
@Table(name = "games")
public class GameEntity {
    protected GameEntity() {}

    public GameEntity(
            UserEntity white,
            UserEntity black,
            GameStatus status,
            Instant createdAt,
            String timeControl,
            EndedBy endedBy
    ){
        this.white = white;
        this.black = black;
        this.status = status;
        this.createdAt = createdAt;
        this.timeControl = timeControl;
        this.endedBy = endedBy;
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

    private String timeControl;

    @Enumerated(EnumType.STRING)
    @Setter
    private EndedBy endedBy;
}
