package com.blikeng.chess.engine;

import com.blikeng.chess.model.*;
import com.blikeng.chess.model.piece.*;

import java.util.List;

/**
 * Applies a move to a {@link Game} and reports the resulting {@link GameStatus}.
 *
 * <p>{@link #performMove} validates the move (correct turn, pseudo-legality, and that it
 * doesn't leave the mover's own king in check), then updates the board for captures,
 * castling, en passant, and promotion. Afterwards it detects game-ending conditions
 * (via {@link GameRules}): checkmate, stalemate, the fifty-move rule, threefold
 * repetition, and insufficient material.
 *
 * <p>Returns {@code null} when the move is illegal; otherwise the new {@link GameStatus}.
 */
public class MoveExecutor {
    private final MoveGenerator moveGenerator = new MoveGenerator();
    private final SquareAttacked squareAttacked = new SquareAttacked(moveGenerator);
    private final GameRules gameRules = new GameRules();

    public GameStatus performMove(Game game, Move move) {
        Board board = game.getBoard();
        Piece piece = board.getPiece(move.from().row(), move.from().col());

        if (piece == null) return null;

        Color color = piece.getColor();
        if (color == Color.WHITE != game.isWhiteTurn()) return null;

        List<Position> legalMoves = moveGenerator.getPseudoLegalMoves(game, board, move.from());
        if (!legalMoves.contains(move.to())) return null;

        boolean isEnPassant = piece.getPieceType() == PieceType.PAWN
                && move.to().equals(game.getEnPassantTarget());

        if (gameRules.kingLeftInCheck(board, game, move, piece, color, isEnPassant)) return null;

        boolean isCapture = board.getPiece(move.to().row(), move.to().col()) != null || isEnPassant;
        if (piece.getPieceType() == PieceType.PAWN || isCapture){
            game.setHalfMoveClock(0);
        } else {
            game.setHalfMoveClock(game.getHalfMoveClock() + 1);
        }

        piece = gameRules.checkIfPawnPromotion(piece, move);

        if (piece.getPieceType() == PieceType.KING){
            handleCastling(board, game, move, piece);
            updateKingPosition(game, move);
        }

        if (isEnPassant) {
            board.setPiece(move.from().row(), move.to().col(), null);
        }

        updateEnPassantTarget(game, piece, move);

        board.setPiece(move.to().row(), move.to().col(), piece);
        board.setPiece(move.from().row(), move.from().col(), null);
        piece.setMoved();

        return gameRules.isGameOver(color, board, game);
    }

    private void handleCastling(Board board, Game game, Move move, Piece piece) {
        int colDiff = move.to().col() - move.from().col();
        if (Math.abs(colDiff) != 2) return;

        if (!canCastle(board, game, colDiff, piece.getColor())) return;

        int row = move.from().row();

        Piece rook;
        if (colDiff == 2) {
            rook = board.getPiece(row, 7);
            board.setPiece(row, 5, rook);
            board.setPiece(row, 7, null);
        } else {
            rook = board.getPiece(row, 0);
            board.setPiece(row, 3, rook);
            board.setPiece(row, 0, null);
        }

        rook.setMoved();
    }

    private boolean canCastle(Board board, Game game, int colDiff, Color color) {
        Color attacker = color == Color.WHITE ? Color.BLACK : Color.WHITE;
        int row = color == Color.WHITE ? 0 : 7;
        int transitCol = colDiff == 2 ? 5 : 3;

        return !squareAttacked.isSquareAttacked(board, game, new Position(row, 4), attacker)
            && !squareAttacked.isSquareAttacked(board, game, new Position(row, transitCol), attacker);
    }

    private void updateKingPosition(Game game, Move move) {
        Position newKingPosition = new Position(move.to().row(), move.to().col());

        if (game.isWhiteTurn()) {
            game.setWhiteKingPosition(newKingPosition);
        } else {
            game.setBlackKingPosition(newKingPosition);
        }
    }

    private void updateEnPassantTarget(Game game, Piece piece, Move move) {
        if (piece.getPieceType() == PieceType.PAWN && Math.abs(move.to().row() - move.from().row()) == 2) {
            int enPassantRow = (move.from().row() + move.to().row()) / 2;
            game.setEnPassantTarget(new Position(enPassantRow, move.from().col()));
        } else {
            game.setEnPassantTarget(null);
        }
    }
}
