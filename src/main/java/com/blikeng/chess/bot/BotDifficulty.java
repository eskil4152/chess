package com.blikeng.chess.bot;

public enum BotDifficulty {
    EASY(1, 300),
    MEDIUM(2, 100),
    HARD(3, 0);

    public final int depth;
    public final int noise;

    BotDifficulty(int depth, int noise) {
        this.depth = depth;
        this.noise = noise;
    }
}