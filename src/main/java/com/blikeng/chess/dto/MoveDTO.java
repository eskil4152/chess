package com.blikeng.chess.dto;

import com.blikeng.chess.model.MoveRecord;

public record MoveDTO (
        String gameId,
        String move
) {
}
