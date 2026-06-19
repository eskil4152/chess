package com.blikeng.chess.model;

import com.blikeng.chess.model.piece.PieceType;

/**
 * A move from one square to another. {@code promotionPiece} is set only for a pawn
 * promotion, otherwise {@code null}.
 */
public record Move(
        Position from, Position to, PieceType promotionPiece
) {
}
