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
        if (piece.getColor() == Color.WHITE != game.isWhiteTurn()) return false;

        List<Position> legalMoves = moveGenerator.getLegalMoves(board, move.from());
        if (!legalMoves.contains(move.to())) return false;

        if (piece.getPieceType() == PieceType.KING) {
            if (game.isWhiteTurn()) {
                game.setWhiteKingPosition(new Position(move.to().row(), move.to().col()));
            } else {
                game.setBlackKingPosition(new Position(move.to().row(), move.to().col()));
            }
        }

        board.setPiece(move.to().row(), move.to().col(), piece);
        board.setPiece(move.from().row(), move.from().col(), null);
        piece.setMoved();

        game.switchTurn();

        return true;
    }
}
