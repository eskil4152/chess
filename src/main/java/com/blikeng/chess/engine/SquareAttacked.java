package com.blikeng.chess.engine;

import com.blikeng.chess.entity.GameEntity;
import com.blikeng.chess.model.Board;
import com.blikeng.chess.model.Position;
import com.blikeng.chess.model.piece.Color;
import com.blikeng.chess.model.piece.Piece;

import java.util.List;

public class SquareAttacked {
    private final MoveGenerator moveGenerator = new MoveGenerator();

    public boolean isInCheck(GameEntity game, Color color){
        Position kingPos = color == Color.WHITE
                ? game.getWhiteKingPosition()
                : game.getBlackKingPosition();

        Color attacker = color == Color.WHITE ? Color.BLACK : Color.WHITE;
        return isSquareAttacked(game.getBoard(), kingPos, attacker);
    }

    public boolean isSquareAttacked (Board board, Position position, Color attackingColor){
        for (int row = 0; row < 8; row++){
            for (int col = 0; col < 7; col++){
                Piece piece = board.getPiece(row, col);

                if (piece == null || piece.getColor() != attackingColor) return false;

                List<Position> moves = moveGenerator.getPseudoLegalMoves(board, new Position(row, col));
                if (moves.contains(position)) return true;
            }
        }

        return false;
    }
}
