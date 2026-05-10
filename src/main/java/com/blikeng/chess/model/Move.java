package com.blikeng.chess.model;

import com.blikeng.chess.model.piece.PieceType;

public record Move(
        Position from, Position to, PieceType promotionPiece
) {
}
