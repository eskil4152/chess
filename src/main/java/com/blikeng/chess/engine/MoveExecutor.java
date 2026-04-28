package com.blikeng.chess.engine;

import com.blikeng.chess.model.Board;
import com.blikeng.chess.model.Game;
import com.blikeng.chess.model.Move;
import com.blikeng.chess.model.Position;
import com.blikeng.chess.model.piece.Color;
import com.blikeng.chess.model.piece.Piece;

import java.util.List;

public class MoveExecutor {
    private final MoveGenerator moveGenerator = new MoveGenerator();

    public void performMove(Game game, Move move) {
        Board board = game.getBoard();
        Piece piece = board.getPiece(move.from().row(), move.from().col());

        if (piece == null) return;
        if (piece.getColor() == Color.WHITE != game.isWhiteTurn()) return;

        List<Position> legalMoves = moveGenerator.getLegalMoves(board, move.from());
        if (!legalMoves.contains(move.to())) return;

        board.setPiece(move.to().row(), move.to().col(), piece);
        board.setPiece(move.from().row(), move.from().col(), null);
        piece.setMoved();

        game.switchTurn();
    }
}
