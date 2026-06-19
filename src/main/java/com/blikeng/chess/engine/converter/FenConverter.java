package com.blikeng.chess.engine.converter;

import com.blikeng.chess.model.Board;
import com.blikeng.chess.model.Game;
import com.blikeng.chess.model.Position;
import com.blikeng.chess.model.piece.Color;
import com.blikeng.chess.model.piece.Piece;
import com.blikeng.chess.model.piece.PieceType;

/**
 * Serializes a {@link Game} to a complete 6-field Forsyth–Edwards Notation (FEN) string:
 * board, side to move, castling rights, en passant target, halfmove clock and fullmove
 * number. Castling rights are derived from the kings' and rooks' moved state.
 */
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
            int empty = appendRow(fen, game.getBoard(), row);
            if (empty > 0) fen.append(empty);
            fen.append(row != 0 ? '/' : ' ');
        }
    }

    private static int appendRow(StringBuilder fen, Board board, int row) {
        int empty = 0;
        for (int col = 0; col < 8; col++) {
            Piece piece = board.getPiece(row, col);
            if (piece == null) {
                empty++;
            } else {
                if (empty > 0) { fen.append(empty); empty = 0; }
                fen.append(pieceToChar(piece));
            }
        }
        return empty;
    }

    private static char pieceToChar(Piece piece) {
        char symbol = PieceType.toChar(piece.getPieceType());
        return piece.getColor() == Color.WHITE
                ? Character.toUpperCase(symbol)
                : Character.toLowerCase(symbol);
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
