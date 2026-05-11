package com.blikeng.chess.engine.converter;

import com.blikeng.chess.model.Board;
import com.blikeng.chess.model.Game;
import com.blikeng.chess.model.piece.Color;
import com.blikeng.chess.model.piece.Piece;
import com.blikeng.chess.model.piece.PieceType;

public class FenConverter {

    public static String toFen(Game game) {
        StringBuilder fen = new StringBuilder();

        appendBoard(fen, game);

        return fen.toString().trim();
    }

    private static void appendBoard(StringBuilder fen, Game game) {
        for (int row = 0; row < 8; row++) {
            int empty = 0;

            for (int col = 0; col < 8; col++) {
                Piece piece = game.getBoard().getPiece(row, col);
                if (piece == null){
                    empty++;
                } else {
                    if (empty > 0){
                        fen.append(empty);
                        empty = 0;
                    }

                    Color color = piece.getColor();
                    char pieceSymbol = PieceType.toChar(piece.getPieceType());
                    String pieceString = String.valueOf(pieceSymbol);

                    if (color == Color.WHITE){
                        fen.append(pieceString.toUpperCase());
                    } else {
                        fen.append(pieceString.toLowerCase());
                    }
                }
            }

            if (empty > 0){
                fen.append(empty);
            }

            if (row != 7){
                fen.append('/');
            } else {
                fen.append(' ');
            }
        }
    }
}
