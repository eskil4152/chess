package com.blikeng.chess.bot;

import java.util.UUID;

/** A bot account: a fixed {@link UUID}, a display name, and its {@link BotDifficulty}. */
public record BotDefinition(UUID id, String username, BotDifficulty difficulty) {}
