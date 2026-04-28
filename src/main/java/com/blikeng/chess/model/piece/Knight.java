package com.blikeng.chess.model.piece;

public class Knight extends Piece {
    public Knight(Color color) {
        super(color);
    }

    @Override
    public PieceType getPieceType() {
        return PieceType.KNIGHT;
    }
}
