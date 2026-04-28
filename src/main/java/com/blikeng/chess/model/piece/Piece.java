package com.blikeng.chess.model.piece;

public abstract class Piece {
    protected Color color;
    protected boolean moved = false;

    public Piece(Color color) {
        this.color = color;
    }

    public Color getColor() {
        return color;
    }

    public boolean hasMoved() {
        return moved;
    }

    public void setMoved(boolean moved) {
        this.moved = moved;
    }
}
