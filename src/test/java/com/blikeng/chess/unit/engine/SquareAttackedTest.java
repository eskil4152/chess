package com.blikeng.chess.unit.engine;

import com.blikeng.chess.engine.MoveGenerator;
import com.blikeng.chess.engine.SquareAttacked;
import com.blikeng.chess.model.Board;
import com.blikeng.chess.model.Game;
import com.blikeng.chess.model.Position;
import com.blikeng.chess.model.piece.Color;
import com.blikeng.chess.model.piece.King;
import com.blikeng.chess.model.piece.Knight;
import com.blikeng.chess.model.piece.Rook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SquareAttackedTest {

    private SquareAttacked squareAttacked;
    private MoveGenerator moveGenerator;
    private Game game;
    private Board board;

    @BeforeEach
    void setup() {
        moveGenerator = new MoveGenerator();
        squareAttacked = new SquareAttacked(moveGenerator);
        game = new Game(UUID.randomUUID(), UUID.randomUUID(), "w", UUID.randomUUID(), "b", 800, 800);
        game.setWhiteKingPosition(new Position(0, 4));
        game.setBlackKingPosition(new Position(7, 4));
        board = game.getBoard();
        clearBoard();
    }

    private void clearBoard() {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                board.setPiece(r, c, null);
    }

    @Test
    void squareShouldBeAttackedByRook() {
        board.setPiece(4, 0, new Rook(Color.BLACK));
        assertThat(squareAttacked.isSquareAttacked(board, game, new Position(4, 7), Color.BLACK)).isTrue();
    }

    @Test
    void squareShouldNotBeAttackedWhenOutOfRange() {
        board.setPiece(4, 0, new Rook(Color.BLACK));
        assertThat(squareAttacked.isSquareAttacked(board, game, new Position(3, 7), Color.BLACK)).isFalse();
    }

    @Test
    void squareShouldBeAttackedByKnight() {
        board.setPiece(2, 3, new Knight(Color.WHITE));
        assertThat(squareAttacked.isSquareAttacked(board, game, new Position(4, 4), Color.WHITE)).isTrue();
    }

    @Test
    void squareShouldNotBeAttackedByOwnColor() {
        board.setPiece(4, 0, new Rook(Color.WHITE));
        assertThat(squareAttacked.isSquareAttacked(board, game, new Position(4, 7), Color.BLACK)).isFalse();
    }

    @Test
    void whiteKingShouldBeInCheck() {
        board.setPiece(0, 4, new King(Color.WHITE));
        board.setPiece(0, 0, new Rook(Color.BLACK));
        game.setWhiteKingPosition(new Position(0, 4));
        assertThat(squareAttacked.isInCheck(game, Color.WHITE)).isTrue();
    }

    @Test
    void whiteKingShouldNotBeInCheck() {
        board.setPiece(0, 4, new King(Color.WHITE));
        game.setWhiteKingPosition(new Position(0, 4));
        assertThat(squareAttacked.isInCheck(game, Color.WHITE)).isFalse();
    }

    @Test
    void blackKingShouldBeInCheck() {
        board.setPiece(7, 4, new King(Color.BLACK));
        board.setPiece(0, 4, new Rook(Color.WHITE));
        game.setBlackKingPosition(new Position(7, 4));
        assertThat(squareAttacked.isInCheck(game, Color.BLACK)).isTrue();
    }

    @Test
    void blackKingShouldNotBeInCheck() {
        board.setPiece(7, 4, new King(Color.BLACK));
        game.setBlackKingPosition(new Position(7, 4));
        assertThat(squareAttacked.isInCheck(game, Color.BLACK)).isFalse();
    }
}
