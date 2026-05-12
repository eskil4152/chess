package com.blikeng.chess.bot;

import java.util.UUID;

public record BotDefinition(UUID id, String username, BotDifficulty difficulty) {}
