package com.blikeng.chess.engine.converter;

import com.blikeng.chess.model.Board;
import com.blikeng.chess.model.Game;
import com.blikeng.chess.model.Position;
import com.blikeng.chess.model.piece.Color;
import com.blikeng.chess.model.piece.Piece;
import com.blikeng.chess.model.piece.PieceType;

public class FenConverter {
    private FenConverter() {}

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
        for (int row = 7; row >= 0; row--) {
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

            if (row != 0){
                fen.append('/');
            } else {
                fen.append(' ');
            }
        }
    }

    private static void appendCastlingRights(StringBuilder fen, Game game) {
        Board board = game.getBoard();

        Piece whiteKing = board.getPiece(game.getWhiteKingPosition().row(), game.getWhiteKingPosition().col());
        Piece blackKing = board.getPiece(game.getBlackKingPosition().row(), game.getBlackKingPosition().col());

        int added = 0;
        if (canCastle(whiteKing, board.getPiece(0, 7), Color.WHITE)) { fen.append('K'); added++; }
        if (canCastle(whiteKing, board.getPiece(0, 0), Color.WHITE)) { fen.append('Q'); added++; }
        if (canCastle(blackKing, board.getPiece(7, 7), Color.BLACK)) { fen.append('k'); added++; }
        if (canCastle(blackKing, board.getPiece(7, 0), Color.BLACK)) { fen.append('q'); added++; }

        if (added == 0) fen.append('-');

        fen.append(' ');
    }

    private static boolean canCastle(Piece king, Piece rook, Color color) {
        return !king.hasMoved()
                && rook != null
                && rook.getPieceType() == PieceType.ROOK
                && rook.getColor() == color
                && !rook.hasMoved();
    }
}
