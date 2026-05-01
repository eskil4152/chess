package com.blikeng.chess.dto;

public record ProfileDTO (
        String username,
        String bio,
        String avatarUrl,
        int elo
){
}
