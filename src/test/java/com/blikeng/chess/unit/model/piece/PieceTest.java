package com.blikeng.chess.unit.model.piece;

import com.blikeng.chess.model.piece.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PieceTest {

    @Test
    void shouldCorrectlyReturnValuesOfPawn() {
        Pawn p = new Pawn(Color.WHITE);
        assertThat(p.getPieceType()).isEqualTo(PieceType.PAWN);
        assertThat(p.getColor()).isEqualTo(Color.WHITE);
        assertThat(p.hasMoved()).isFalse();
        p.setMoved();
        assertThat(p.hasMoved()).isTrue();
    }

    @Test
    void shouldReturnCorrectTypeForRook() {
        Rook r = new Rook(Color.BLACK);
        assertThat(r.getPieceType()).isEqualTo(PieceType.ROOK);
        assertThat(r.getColor()).isEqualTo(Color.BLACK);
    }

    @Test
    void shouldReturnCorrectTypeForKnight() {
        assertThat(new Knight(Color.WHITE).getPieceType()).isEqualTo(PieceType.KNIGHT);
    }

    @Test
    void shouldReturnCorrectTypeForBishop() {
        assertThat(new Bishop(Color.BLACK).getPieceType()).isEqualTo(PieceType.BISHOP);
    }

    @Test
    void shouldReturnCorrectTypeForQueen() {
        assertThat(new Queen(Color.WHITE).getPieceType()).isEqualTo(PieceType.QUEEN);
    }

    @Test
    void shouldReturnCorrectTypeForKing() {
        assertThat(new King(Color.BLACK).getPieceType()).isEqualTo(PieceType.KING);
    }

    @Test
    void toCharShouldReturnCorrectCharacter() {
        assertThat(PieceType.toChar(PieceType.ROOK)).isEqualTo('R');
        assertThat(PieceType.toChar(PieceType.KNIGHT)).isEqualTo('N');
        assertThat(PieceType.toChar(PieceType.BISHOP)).isEqualTo('B');
        assertThat(PieceType.toChar(PieceType.QUEEN)).isEqualTo('Q');
        assertThat(PieceType.toChar(PieceType.KING)).isEqualTo('K');
        assertThat(PieceType.toChar(PieceType.PAWN)).isEqualTo('P');
    }

    @Test
    void fromCharShouldReturnCorrectPieceType() {
        assertThat(PieceType.fromChar('r')).isEqualTo(PieceType.ROOK);
        assertThat(PieceType.fromChar('R')).isEqualTo(PieceType.ROOK);
        assertThat(PieceType.fromChar('n')).isEqualTo(PieceType.KNIGHT);
        assertThat(PieceType.fromChar('b')).isEqualTo(PieceType.BISHOP);
        assertThat(PieceType.fromChar('q')).isEqualTo(PieceType.QUEEN);
    }

    @Test
    void fromCharShouldThrowOnInvalidChar() {
        assertThatThrownBy(() -> PieceType.fromChar('x'))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PieceType.fromChar('k'))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PieceType.fromChar('p'))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
