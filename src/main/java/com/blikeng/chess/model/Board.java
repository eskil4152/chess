package com.blikeng.chess.model;

import com.blikeng.chess.model.piece.Knight;
import com.blikeng.chess.model.piece.Piece;

import static com.blikeng.chess.engine.SetupBoard.setupBoard;

/**
 * An 8×8 grid of squares, each holding a {@link Piece} or {@code null} when empty
 * (row 0 = rank 1).
 *
 * <p>The default constructor sets up the standard starting position; the copy constructor
 * deep-copies every piece (via {@link Piece#copy}) so simulated games don't share state -
 * e.g. checking move legality copies the board, plays the move, and tests whether the
 * mover's king is left in check, all without touching the live board.
 * {@link #toString} renders a compact text grid (knights shown as {@code N} to avoid
 * clashing with the king's {@code K}) and doubles as the board signature in the
 * repetition key.
 */
public class Board {
    private final Piece[][] squares = new Piece[8][8];

    public Board(Board board) {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board.squares[row][col];
                this.squares[row][col] = piece != null ? piece.copy() : null;
            }
        }
    }

    public Board() {
        setupBoard(squares);
    }

    public Piece getPiece(int row, int col) {
        return squares[row][col];
    }

    public void setPiece(int row, int col, Piece piece) {
        squares[row][col] = piece;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = squares[row][col];
                if (piece != null && piece.getClass() == Knight.class){
                    sb.append('N');
                } else {
                    sb.append(piece == null ? "." : piece.getClass().getSimpleName().charAt(0));
                }

                sb.append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
