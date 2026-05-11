package com.blikeng.chess.engine.converter;

import com.blikeng.chess.engine.PositionMapper;
import com.blikeng.chess.model.Board;
import com.blikeng.chess.model.Game;
import com.blikeng.chess.model.Position;
import com.blikeng.chess.model.piece.Color;
import com.blikeng.chess.model.piece.Piece;
import com.blikeng.chess.model.piece.PieceType;

public class FenConverter {
    public static String toFen(Game game) {
        StringBuilder fen = new StringBuilder();

        appendBoard(fen, game);
        fen.append(game.isWhiteTurn() ? "w" : "b").append(' ');
        appendCastlingRights(fen, game);

        Position enPassantTarget = game.getEnPassantTarget();
        if (enPassantTarget == null){
            fen.append('-');
        } else {
            fen.append((char) ('a' + enPassantTarget.col()));
            fen.append(enPassantTarget.row() + 1);
        }

        fen.append(' ');

        fen.append(game.getHalfMoveClock()).append(' ');

        fen.append((game.getMoves().size() / 2) + 1);

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

    private static void appendCastlingRights(StringBuilder fen, Game game) {
        Board board = game.getBoard();

        int added = 0;

        Piece whiteKing = board.getPiece(game.getWhiteKingPosition().row(), game.getWhiteKingPosition().col());
        Piece blackKing = board.getPiece(game.getBlackKingPosition().row(), game.getBlackKingPosition().col());

        Piece whiteKingsideRook = board.getPiece(0, 7);
        Piece whiteQueensideRook = board.getPiece(0, 0);

        Piece blackKingsideRook = board.getPiece(7, 7);
        Piece blackQueensideRook = board.getPiece(7, 0);

        if (
                !whiteKing.hasMoved() &&
                whiteKingsideRook != null &&
                whiteKingsideRook.getPieceType() == PieceType.ROOK &&
                whiteKingsideRook.getColor() == Color.WHITE &&
                !whiteKingsideRook.hasMoved()
        ) {
            fen.append('K');
            added++;
        }

        if (
                !whiteKing.hasMoved() &&
                whiteQueensideRook != null &&
                whiteQueensideRook.getPieceType() == PieceType.ROOK &&
                whiteQueensideRook.getColor() == Color.WHITE &&
                !whiteQueensideRook.hasMoved()
        ){
            fen.append('Q');
            added++;
        }

        if (
                !blackKing.hasMoved() &&
                blackKingsideRook != null &&
                blackKingsideRook.getPieceType() == PieceType.ROOK &&
                blackKingsideRook.getColor() == Color.BLACK &&
                !blackKingsideRook.hasMoved()
        ) {
            fen.append('k');
            added++;
        }

        if (
                !blackKing.hasMoved() &&
                blackQueensideRook != null &&
                blackQueensideRook.getPieceType() == PieceType.ROOK &&
                blackQueensideRook.getColor() == Color.BLACK &&
                !blackQueensideRook.hasMoved()
        ) {
            fen.append('q');
            added++;
        }

        if (added == 0){
            fen.append('-');
        }

        fen.append(' ');
    }
}
