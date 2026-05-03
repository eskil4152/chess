package com.blikeng.chess.unit.engine;

import com.blikeng.chess.model.Board;
import com.blikeng.chess.model.piece.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SetupBoardTest {

    @Test
    void shouldPlaceWhiteMajorPiecesOnRank1() {
        Board b = new Board();
        assertThat(b.getPiece(7, 0)).isInstanceOf(Rook.class).extracting(Piece::getColor).isEqualTo(Color.WHITE);
        assertThat(b.getPiece(7, 1)).isInstanceOf(Knight.class).extracting(Piece::getColor).isEqualTo(Color.WHITE);
        assertThat(b.getPiece(7, 2)).isInstanceOf(Bishop.class).extracting(Piece::getColor).isEqualTo(Color.WHITE);
        assertThat(b.getPiece(7, 3)).isInstanceOf(Queen.class).extracting(Piece::getColor).isEqualTo(Color.WHITE);
        assertThat(b.getPiece(7, 4)).isInstanceOf(King.class).extracting(Piece::getColor).isEqualTo(Color.WHITE);
        assertThat(b.getPiece(7, 5)).isInstanceOf(Bishop.class).extracting(Piece::getColor).isEqualTo(Color.WHITE);
        assertThat(b.getPiece(7, 6)).isInstanceOf(Knight.class).extracting(Piece::getColor).isEqualTo(Color.WHITE);
        assertThat(b.getPiece(7, 7)).isInstanceOf(Rook.class).extracting(Piece::getColor).isEqualTo(Color.WHITE);
    }

    @Test
    void shouldPlaceBlackMajorPiecesOnRank8() {
        Board b = new Board();
        assertThat(b.getPiece(0, 0)).isInstanceOf(Rook.class).extracting(Piece::getColor).isEqualTo(Color.BLACK);
        assertThat(b.getPiece(0, 1)).isInstanceOf(Knight.class).extracting(Piece::getColor).isEqualTo(Color.BLACK);
        assertThat(b.getPiece(0, 2)).isInstanceOf(Bishop.class).extracting(Piece::getColor).isEqualTo(Color.BLACK);
        assertThat(b.getPiece(0, 3)).isInstanceOf(Queen.class).extracting(Piece::getColor).isEqualTo(Color.BLACK);
        assertThat(b.getPiece(0, 4)).isInstanceOf(King.class).extracting(Piece::getColor).isEqualTo(Color.BLACK);
        assertThat(b.getPiece(0, 5)).isInstanceOf(Bishop.class).extracting(Piece::getColor).isEqualTo(Color.BLACK);
        assertThat(b.getPiece(0, 6)).isInstanceOf(Knight.class).extracting(Piece::getColor).isEqualTo(Color.BLACK);
        assertThat(b.getPiece(0, 7)).isInstanceOf(Rook.class).extracting(Piece::getColor).isEqualTo(Color.BLACK);
    }

    @Test
    void shouldPlacePawnsOnRanks2And7() {
        Board b = new Board();
        for (int col = 0; col < 8; col++) {
            assertThat(b.getPiece(6, col)).isInstanceOf(Pawn.class).extracting(Piece::getColor).isEqualTo(Color.WHITE);
            assertThat(b.getPiece(1, col)).isInstanceOf(Pawn.class).extracting(Piece::getColor).isEqualTo(Color.BLACK);
        }
    }

    @Test
    void shouldLeaveMiddleRowsEmpty() {
        Board b = new Board();
        for (int row = 2; row <= 5; row++) {
            for (int col = 0; col < 8; col++) {
                assertThat(b.getPiece(row, col)).isNull();
            }
        }
    }

    @Test
    void allPiecesShouldStartAsNotMoved() {
        Board b = new Board();
        for (int row : new int[]{0, 1, 6, 7}) {
            for (int col = 0; col < 8; col++) {
                assertThat(b.getPiece(row, col).hasMoved())
                        .as("piece at (%d,%d) should not have moved", row, col)
                        .isFalse();
            }
        }
    }
}
