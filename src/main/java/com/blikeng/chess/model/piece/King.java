package com.blikeng.chess.model.piece;

public class King extends Piece {
    public King(Color color) {
        super(color);
    }

    @Override
    public PieceType getPieceType() {
        return PieceType.KING;
    }
}
