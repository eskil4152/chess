package com.blikeng.chess.engine;

import com.blikeng.chess.model.Move;
import com.blikeng.chess.model.Position;
import com.blikeng.chess.model.piece.PieceType;

/**
 * Converts between board coordinates and string notation.
 *
 * <p>A {@link Position} uses {@code row = rank - 1} and {@code col = file} (both 0-7, so
 * {@code a1} is row 0, col 0). Handles algebraic squares (e.g. {@code "e4"}) and UCI
 * moves (e.g. {@code "e7e8q"}).
 */
public class PositionMapper {
    private PositionMapper() {}

    public static Position fromString(String s) {
        int col = s.charAt(0) - 'a';
        int row = (s.charAt(1) - '0') - 1;
        return new Position(row, col);
    }

    public static String toString(Position position) {
        return "" + (char)('a' + position.col()) + (position.row() + 1);
    }

    public static Move fromUci(String uciMove){
        String from = uciMove.substring(0, 2);
        String to = uciMove.substring(2, 4);
        PieceType promotion = null;

        if (uciMove.length() == 5) promotion = PieceType.fromChar(uciMove.charAt(4));

        return new Move(
          fromString(from),
          fromString(to),
          promotion
        );
    }
}
