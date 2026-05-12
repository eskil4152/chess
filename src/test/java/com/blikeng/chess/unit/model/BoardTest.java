package com.blikeng.chess.unit.model;

import com.blikeng.chess.model.Board;
import com.blikeng.chess.model.piece.Color;
import com.blikeng.chess.model.piece.Queen;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BoardTest {

    @Test
    void copyConstructorShouldDeepCopyAllSquares() {
        Board original = new Board();
        Board copy = new Board(original);

        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++) {
                if (original.getPiece(r, c) == null) {
                    assertThat(copy.getPiece(r, c)).isNull();
                } else {
                    assertThat(copy.getPiece(r, c)).isNotSameAs(original.getPiece(r, c));
                    assertThat(copy.getPiece(r, c).getPieceType()).isEqualTo(original.getPiece(r, c).getPieceType());
                    assertThat(copy.getPiece(r, c).getColor()).isEqualTo(original.getPiece(r, c).getColor());
                }
            }

        copy.setPiece(0, 0, null);
        assertThat(original.getPiece(0, 0)).isNotNull();
    }

    @Test
    void copyConstructorShouldNotShareMovedState() {
        Board original = new Board();
        original.getPiece(1, 0).setMoved();
        Board copy = new Board(original);

        copy.getPiece(6, 0).setMoved();
        assertThat(original.getPiece(6, 0).hasMoved()).isFalse();
    }

    @Test
    void shouldSetPieceToActualPiece() {
        Board b = new Board();
        Queen q = new Queen(Color.WHITE);
        b.setPiece(3, 3, q);
        assertThat(b.getPiece(3, 3)).isSameAs(q);
    }

    @Test
    void shouldClearSquare() {
        Board b = new Board();
        b.setPiece(6, 0, null);
        assertThat(b.getPiece(6, 0)).isNull();
    }

    @Test
    void shouldConvertSquaresToLetters() {
        Board b = new Board();
        String s = b.toString();
        assertThat(s)
                .contains(".", "N", "B", "R", "Q", "K", "B", "N");
    }
}
