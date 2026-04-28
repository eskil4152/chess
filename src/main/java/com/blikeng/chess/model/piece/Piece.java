package com.blikeng.chess.model.piece;

public abstract class Piece {
    protected Color color;
    protected boolean moved = false;

    public Piece(Color color) {
        this.color = color;
    }

    public abstract PieceType getPieceType();

    public Color getColor() {
        return color;
    }

    public boolean hasMoved() {
        return moved;
    }

    public void setMoved() {
        this.moved = true;
    }
}
