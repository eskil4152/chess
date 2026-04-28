package com.blikeng.chess.model.piece;

public class Bishop extends Piece {
    public Bishop(Color color) {
        super(color);
    }

    @Override
    public PieceType getPieceType() {
        return PieceType.BISHOP;
    }
}
