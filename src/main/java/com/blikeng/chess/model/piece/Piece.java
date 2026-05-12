package com.blikeng.chess.model.piece;

import lombok.Getter;

public abstract class Piece {
    @Getter
    protected Color color;

    protected boolean moved = false;

    protected Piece(Color color) {
        this.color = color;
    }

    public abstract PieceType getPieceType();

    public boolean hasMoved() {
        return moved;
    }

    public void setMoved() {
        this.moved = true;
    }

    public Piece copy() {
        Piece copy = switch (getPieceType()) {
            case PAWN -> new Pawn(color);
            case KNIGHT -> new Knight(color);
            case BISHOP -> new Bishop(color);
            case ROOK -> new Rook(color);
            case QUEEN -> new Queen(color);
            case KING -> new King(color);
        };

        if (moved) copy.setMoved();
        return copy;
    }
}