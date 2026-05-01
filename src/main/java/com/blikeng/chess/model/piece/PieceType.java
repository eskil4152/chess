package com.blikeng.chess.model.piece;

public enum PieceType {
    ROOK, KNIGHT, BISHOP, QUEEN, KING, PAWN;

    public static PieceType fromChar(char c) {
        return switch (Character.toLowerCase(c)) {
            case 'r' -> ROOK;
            case 'n' -> KNIGHT;
            case 'b' -> BISHOP;
            case 'q' -> QUEEN;
            default -> throw new IllegalArgumentException("Unknown promotion piece: " + c);
        };
    }
}
