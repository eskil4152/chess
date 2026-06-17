package com.blikeng.chess.bot;

/**
 * Bot difficulty levels, tuned by engine search {@code depth} and {@code noise}.
 *
 * <p>{@code noise} is a random +- centipawn jitter added to move scores, causing the
 * bot to deviate from the ideal move. Higher depth = stronger, higher noise = weaker.
 * {@code HARD} has zero noise and always plays the best move found.
 */
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