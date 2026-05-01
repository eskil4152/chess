package com.blikeng.chess.engine;

import com.blikeng.chess.model.Position;

public class PositionMapper {
    public static Position fromString(String s) {
        int col = s.charAt(0) - 'a';
        int row = 8 - (s.charAt(1) - '0');
        return new Position(row, col);
    }

    public static String toString(Position position) {
        return "" + (char)('a' + position.col()) + (8 - position.row());
    }
}
