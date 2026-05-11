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
}
