package com.blikeng.chess.model;

import com.blikeng.chess.model.piece.Knight;
import com.blikeng.chess.model.piece.Piece;

import static com.blikeng.chess.engine.SetupBoard.setupBoard;

public class Board {
    private final Piece[][] squares = new Piece[8][8];

    public Board() {
        setupBoard(squares);
    }

    public Piece getPiece(int x, int y) {
        return squares[x][y];
    }

    public void setPiece(int x, int y, Piece piece) {
        squares[x][y] = piece;
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
