package com.blikeng.chess.dto;

/** A user's public profile: bio/avatar, per-time-control stats, friend flag, and active game id. */
public record ProfileDTO (
        String username,
        String bio,
        String avatarUrl,

        int bulletElo,
        int bulletGames,
        int bulletWins,
        double bulletWinPercentage,

        int blitzElo,
        int blitzGames,
        int blitzWins,
        double blitzWinPercentage,

        int rapidElo,
        int rapidGames,
        int rapidWins,
        double rapidWinPercentage,

        int classicalElo,
        int classicalGames,
        int classicalWins,
        double classicalWinPercentage,

        boolean isFriend,
        String activeGameId
){
}
