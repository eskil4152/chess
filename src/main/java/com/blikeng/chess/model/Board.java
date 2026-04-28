package com.blikeng.chess.model;

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
}
