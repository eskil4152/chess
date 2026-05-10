package com.blikeng.chess.engine;

import com.blikeng.chess.model.Move;
import com.blikeng.chess.model.Position;
import com.blikeng.chess.model.piece.PieceType;

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
