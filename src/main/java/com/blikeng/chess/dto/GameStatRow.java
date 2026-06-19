package com.blikeng.chess.dto;

import com.blikeng.chess.model.EndedBy;
import com.blikeng.chess.model.GameStatus;

import java.util.UUID;

/**
 * Minimal per-game projection used to aggregate player statistics: which side the player
 * was (via the white player's id), the result, and how the game ended. Avoids loading full
 * {@link com.blikeng.chess.entity.GameEntity} rows (and the heavy moves/PGN column) just to count outcomes.
 */
public record GameStatRow(UUID whiteId, GameStatus status, EndedBy endedBy) {}