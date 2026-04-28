package com.blikeng.chess.model.piece;

public class Queen extends Piece {
    public Queen(Color color) {
        super(color);
    }

    @Override
    public PieceType getPieceType() {
        return PieceType.QUEEN;
    }
}
