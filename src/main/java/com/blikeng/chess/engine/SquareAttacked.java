package com.blikeng.chess.engine;

import com.blikeng.chess.model.Board;
import com.blikeng.chess.model.Game;
import com.blikeng.chess.model.Position;
import com.blikeng.chess.model.piece.Color;
import com.blikeng.chess.model.piece.Piece;

import java.util.List;

/**
 * Detects whether a square is attacked - used for check detection and castling safety.
 *
 * <p>A square counts as attacked if any enemy piece has a pseudo-legal move onto it
 * (via {@link MoveGenerator}). {@link #isInCheck} applies this to a king's square.
 */
public class SquareAttacked {
    private final MoveGenerator moveGenerator;

    public SquareAttacked(MoveGenerator moveGenerator) {
        this.moveGenerator = moveGenerator;
    }

    public boolean isInCheck(Game game, Color color){
        Position kingPos = color == Color.WHITE
                ? game.getWhiteKingPosition()
                : game.getBlackKingPosition();

        Color attacker = color == Color.WHITE ? Color.BLACK : Color.WHITE;
        return isSquareAttacked(game.getBoard(), game, kingPos, attacker);
    }

    public boolean isSquareAttacked(Board board, Game game, Position position, Color attackingColor){
        for (int row = 0; row < 8; row++){
            for (int col = 0; col < 8; col++){
                Piece piece = board.getPiece(row, col);

                if (piece == null || piece.getColor() != attackingColor) continue;

                List<Position> moves = moveGenerator.getPseudoLegalMoves(game, board, new Position(row, col));
                if (moves.contains(position)) return true;
            }
        }

        return false;
    }
}
