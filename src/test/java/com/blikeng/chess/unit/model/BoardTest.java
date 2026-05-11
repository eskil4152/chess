package com.blikeng.chess.unit.model;

import com.blikeng.chess.model.Board;
import com.blikeng.chess.model.piece.Color;
import com.blikeng.chess.model.piece.Queen;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BoardTest {

    @Test
    void copyConstructorShouldCopyAllSquares() {
        Board original = new Board();
        Board copy = new Board(original);
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                assertThat(copy.getPiece(r, c)).isSameAs(original.getPiece(r, c));

        copy.setPiece(0, 0, null);
        assertThat(original.getPiece(0, 0)).isNotNull();
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
