package com.blikeng.chess.model.piece;

public class Pawn extends Piece {
    public Pawn(Color color) {
        super(color);
    }

    @Override
    public PieceType getPieceType() {
        return PieceType.PAWN;
    }
}
