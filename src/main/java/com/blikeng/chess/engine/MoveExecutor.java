package com.blikeng.chess.engine;

import com.blikeng.chess.model.Board;
import com.blikeng.chess.entity.GameEntity;
import com.blikeng.chess.model.Move;
import com.blikeng.chess.model.Position;
import com.blikeng.chess.model.piece.Color;
import com.blikeng.chess.model.piece.Piece;
import com.blikeng.chess.model.piece.PieceType;

import java.util.List;

public class MoveExecutor {
    private final MoveGenerator moveGenerator = new MoveGenerator();
    private final SquareAttacked squareAttacked = new SquareAttacked();

    public boolean performMove(GameEntity game, Move move) {
        Board board = game.getBoard();
        Piece piece = board.getPiece(move.from().row(), move.from().col());

        if (piece == null) return false;

        Color color = piece.getColor();
        if (color == Color.WHITE != game.isWhiteTurn()) return false;

        List<Position> legalMoves = moveGenerator.getLegalMoves(board, move.from());
        if (!legalMoves.contains(move.to())) return false;

        if (kingLeftInCheck(board, game, move, piece, color)) return false;

        if (piece.getPieceType() == PieceType.KING) {
            Position newKingPosition = new Position(move.to().row(), move.to().col());

            if (game.isWhiteTurn()) {
                game.setWhiteKingPosition(newKingPosition);
            } else {
                game.setBlackKingPosition(newKingPosition);
            }
        }

        board.setPiece(move.to().row(), move.to().col(), piece);
        board.setPiece(move.from().row(), move.from().col(), null);
        piece.setMoved();

        game.switchTurn();

        return true;
    }

    private boolean kingLeftInCheck(Board board, GameEntity game, Move move, Piece piece, Color color) {
        Board copy = new Board(board);
        copy.setPiece(move.to().row(), move.to().col(), piece);
        copy.setPiece(move.from().row(), move.from().col(), null);

        Position kingPos = color == Color.WHITE
                ? game.getWhiteKingPosition()
                : game.getBlackKingPosition();

        if (piece.getPieceType() == PieceType.KING) {
            kingPos = move.to();
        }

        Color attacker = color == Color.WHITE ? Color.BLACK : Color.WHITE;

        return squareAttacked.isSquareAttacked(copy, kingPos, attacker);
    }
}
