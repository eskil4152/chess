package com.blikeng.chess.engine;

import com.blikeng.chess.model.piece.*;

import static com.blikeng.chess.model.piece.Color.BLACK;
import static com.blikeng.chess.model.piece.Color.WHITE;

/**
 * Places all pieces in the standard chess starting position.
 *
 * <p>White occupies rows 0-1, and Black rows 6-7 (row 0 = rank 1).
 */
public class SetupBoard {
    private SetupBoard() {}

    public static void setupBoard(Piece[][] squares) {
        squares[0][0] = new Rook(WHITE);
        squares[0][1] = new Knight(WHITE);
        squares[0][2] = new Bishop(WHITE);
        squares[0][3] = new Queen(WHITE);
        squares[0][4] = new King(WHITE);
        squares[0][5] = new Bishop(WHITE);
        squares[0][6] = new Knight(WHITE);
        squares[0][7] = new Rook(WHITE);

        for (int col = 0; col < 8; col++) {
            squares[1][col] = new Pawn(WHITE);
        }

        for (int col = 0; col < 8; col++) {
            squares[6][col] = new Pawn(BLACK);
        }

        squares[7][0] = new Rook(BLACK);
        squares[7][1] = new Knight(BLACK);
        squares[7][2] = new Bishop(BLACK);
        squares[7][3] = new Queen(BLACK);
        squares[7][4] = new King(BLACK);
        squares[7][5] = new Bishop(BLACK);
        squares[7][6] = new Knight(BLACK);
        squares[7][7] = new Rook(BLACK);
    }
}
