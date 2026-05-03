package com.blikeng.chess.engine;

import com.blikeng.chess.model.Position;

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
}
