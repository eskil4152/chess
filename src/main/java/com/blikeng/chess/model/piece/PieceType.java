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

    public static char toChar(PieceType pieceType) {
        return switch (pieceType) {
            case ROOK -> 'R';
            case KNIGHT -> 'N';
            case BISHOP -> 'B';
            case QUEEN -> 'Q';
            default -> ' ';
        };
    }

    public int getPieceValue() {
        return switch (this) {
            case PAWN -> 100;
            case KNIGHT -> 350;
            case BISHOP -> 350;
            case ROOK -> 525;
            case QUEEN -> 1000;
            case KING -> 10000;
        };
    }
}
