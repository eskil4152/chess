package com.blikeng.chess.dto;

import com.blikeng.chess.model.piece.PieceType;

public record MoveDTO (
        String gameId,
        String fromPos,
        String toPos,
        PieceType promotionPiece
) {
}
