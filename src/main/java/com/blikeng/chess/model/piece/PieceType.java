package com.blikeng.chess.model.piece;

/**
 * The six piece types, with notation-character conversions and material values.
 *
 * <p>{@link #toChar} maps every type to its standard letter (R, N, B, Q, K, P);
 * {@link #fromChar} is the inverse but only accepts promotion pieces (q, r, b, n) and
 * throws otherwise. {@link #getPieceValue} returns centipawn material values used by the
 * evaluator (pawn 100, …, queen 1000, king 10000).
 */
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
            case KING -> 'K';
            case PAWN -> 'P';
        };
    }

    public int getPieceValue() {
        return switch (this) {
            case PAWN -> 100;
            case ROOK -> 525;
            case QUEEN -> 1000;
            case KING -> 10000;
            default -> 350;
        };
    }
}
