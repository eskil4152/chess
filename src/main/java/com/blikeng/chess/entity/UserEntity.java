package com.blikeng.chess.entity;

import com.blikeng.chess.model.timecontrol.TimeControl;
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

    private int bulletGames = 0;
    private int bulletWins = 0;
    private int bulletElo = 800;
    @Column(name = "been_2400_bullet")
    private boolean been2400Bullet = false;

    private int blitzGames = 0;
    private int blitzWins = 0;
    private int blitzElo = 800;
    @Column(name = "been_2400_blitz")
    private boolean been2400Blitz = false;

    private int rapidGames = 0;
    private int rapidWins = 0;
    private int rapidElo = 800;
    @Column(name = "been_2400_rapid")
    private boolean been2400Rapid = false;

    private int classicalGames = 0;
    private int classicalWins = 0;
    private int classicalElo = 800;
    @Column(name = "been_2400_classical")
    private boolean been2400Classical = false;

    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.USER;

    public int getElo(TimeControl timeControl){
        return switch (timeControl.type()) {
            case BULLET -> bulletElo;
            case BLITZ -> blitzElo;
            case RAPID -> rapidElo;
            case CLASSICAL -> classicalElo;
        };
    }

    public double getWinPercentage(String timeControl){
        double winPercentage = 100 * switch (timeControl) {
            case "bullet" -> bulletWins / (double) bulletGames;
            case "blitz" -> blitzWins / (double) blitzGames;
            case "rapid" -> rapidWins / (double) rapidGames;
            case "classical" -> classicalWins / (double) classicalGames;
            default -> 0;
        };

        return Double.isNaN(winPercentage) ? 0 : winPercentage;
    }
}
