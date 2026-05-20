package com.blikeng.chess.dto;

public record ProfileDTO (
        String username,
        String bio,
        String avatarUrl,
        int bulletElo,
        int bulletGames,
        int blitzElo,
        int blitzGames,
        int rapidElo,
        int rapidGames,
        boolean isFriend
){
}
