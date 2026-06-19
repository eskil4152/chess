package com.blikeng.chess.dto;

import java.util.UUID;

/** A leaderboard row: identity plus games/wins/win% and Elo for one time control. */
public class LeaderboardPlayerDTO {
    public UUID id;
    public String username;
    public int games;
    public int wins;
    public int winPercentage;
    public int elo;

    public LeaderboardPlayerDTO(UUID id, String username, int games, int wins, int elo) {
        this.id = id;
        this.username = username;
        this.games = games;
        this.wins = wins;
        this.elo = elo;
        this.winPercentage = getWinPercentage();
    }

    public int getWinPercentage() {
        return (int) (100 * (double) wins / games);
    }
}
