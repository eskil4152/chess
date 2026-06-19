package com.blikeng.chess.dto;

/** Detailed per-time-control stats: win/loss/draw counts broken down by ending type and by colour. */
public record PlayerStatsDTO(
    int elo,

    int gamesWon,
    int gamesLost,
    int gamesDrawn,
    int gamesPlayed,

    int winsByCheckmate,
    int winsByFlagging,
    int winsByResignation,

    int lossesByCheckmate,
    int lossesByFlagging,
    int lossesByResignation,

    int drawsByStalemate,
    int drawsByAgreement,
    int drawsByRepetition,
    int drawsBy50MoveRule,
    int drawsByInsufficientMaterial,

    int gamesAsBlack,
    int winsAsBlack,
    int lossesAsBlack,

    int gamesAsWhite,
    int winsAsWhite,
    int lossesAsWhite
) {
}