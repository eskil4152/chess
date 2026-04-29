package com.blikeng.chess.model;

import com.blikeng.chess.model.piece.PieceType;

public record MoveRecord (
        Move move,
        PieceType pieceType,
        boolean isEnPassant,
        boolean isCastling
){
}
