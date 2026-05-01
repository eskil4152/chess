package com.blikeng.chess.dto;

import com.blikeng.chess.model.GameStatus;

import java.util.UUID;

public record GameOverDTO (
        UUID gameId,
        GameStatus status
) {
}
