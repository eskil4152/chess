package com.blikeng.chess.dto;

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
