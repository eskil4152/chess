package com.blikeng.chess.model.piece;

import lombok.Getter;

/**
 * Abstract base for the six chess pieces.
 *
 * <p>Holds the piece's {@link Color} and a {@code moved} flag (used for castling rights,
 * the pawn's two-square first move, and en passant). Each concrete subtype ({@link Pawn},
 * {@link Knight}, {@link Bishop}, {@link Rook}, {@link Queen}, {@link King}) only declares
 * its {@link PieceType}. Movement rules live in
 * {@link com.blikeng.chess.engine.MoveGenerator}, not on the piece.
 *
 * <p>{@link #copy} returns a deep copy that preserves the {@code moved} flag.
 */
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