package com.blikeng.chess.model;

/**
 * A board square as {@code row} (rank − 1) and {@code col} (file, a = 0), both 0-7.
 */
public record Position(
    int row,
    int col
) {}
