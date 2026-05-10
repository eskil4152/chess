package com.blikeng.chess.unit.engine;

import com.blikeng.chess.engine.MoveGenerator;
import com.blikeng.chess.model.Board;
import com.blikeng.chess.model.Game;
import com.blikeng.chess.model.Position;
import com.blikeng.chess.model.piece.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MoveGeneratorTest {

    private MoveGenerator gen;
    private Game game;
    private Board board;

    @BeforeEach
    void setup() {
        gen = new MoveGenerator();
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

    // --- null piece ---

    @Test
    void shouldReturnEmptyWhenNoPieceAtPosition() {
        assertThat(gen.getPseudoLegalMoves(game, board, new Position(4, 4))).isEmpty();
    }

    // --- Rook ---

    @Test
    void rookShouldSlideInFourDirections() {
        board.setPiece(4, 4, new Rook(Color.WHITE));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(4, 4));
        assertThat(moves).hasSize(14);
    }

    @Test
    void rookShouldStopBeforeOwnPiece() {
        board.setPiece(4, 4, new Rook(Color.WHITE));
        board.setPiece(4, 6, new Pawn(Color.WHITE));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(4, 4));
        assertThat(moves).doesNotContain(new Position(4, 6));
        assertThat(moves).doesNotContain(new Position(4, 7));
        assertThat(moves).contains(new Position(4, 5));
    }

    @Test
    void rookShouldCaptureEnemyAndStop() {
        board.setPiece(4, 4, new Rook(Color.WHITE));
        board.setPiece(4, 6, new Pawn(Color.BLACK));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(4, 4));
        assertThat(moves).contains(new Position(4, 6));
        assertThat(moves).doesNotContain(new Position(4, 7));
    }

    // --- Knight ---

    @Test
    void knightShouldHaveEightMovesFromCenter() {
        board.setPiece(4, 4, new Knight(Color.WHITE));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(4, 4));
        assertThat(moves).hasSize(8);
    }

    @Test
    void knightShouldBeRestrictedFromCorner() {
        board.setPiece(0, 0, new Knight(Color.WHITE));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(0, 0));
        assertThat(moves).hasSize(2);
    }

    @Test
    void knightShouldSkipOwnPieceSquare() {
        board.setPiece(4, 4, new Knight(Color.WHITE));
        board.setPiece(2, 5, new Pawn(Color.WHITE));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(4, 4));
        assertThat(moves).doesNotContain(new Position(2, 5));
        assertThat(moves).hasSize(7);
    }

    @Test
    void knightShouldCaptureEnemy() {
        board.setPiece(4, 4, new Knight(Color.WHITE));
        board.setPiece(2, 5, new Pawn(Color.BLACK));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(4, 4));
        assertThat(moves).contains(new Position(2, 5));
    }

    // --- Bishop ---

    @Test
    void bishopShouldSlideDiagonally() {
        board.setPiece(4, 4, new Bishop(Color.WHITE));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(4, 4));
        assertThat(moves).hasSize(13);
    }

    // --- Queen ---

    @Test
    void queenShouldCombineRookAndBishopMoves() {
        board.setPiece(4, 4, new Queen(Color.WHITE));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(4, 4));
        assertThat(moves).hasSize(27);
    }

    // --- King ---

    @Test
    void kingShouldMoveToAdjacentSquares() {
        board.setPiece(4, 4, new King(Color.WHITE));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(4, 4));
        assertThat(moves).hasSize(8);
    }

    @Test
    void kingShouldNotCaptureOwnPiece() {
        board.setPiece(4, 4, new King(Color.WHITE));
        board.setPiece(3, 4, new Pawn(Color.WHITE));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(4, 4));
        assertThat(moves).doesNotContain(new Position(3, 4));
        assertThat(moves).hasSize(7);
    }

    @Test
    void kingShouldCaptureEnemy() {
        board.setPiece(4, 4, new King(Color.WHITE));
        board.setPiece(3, 4, new Pawn(Color.BLACK));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(4, 4));
        assertThat(moves).contains(new Position(3, 4));
    }

    @Test
    void whiteKingShouldIncludeKingsideCastleWhenAllowed() {
        King king = new King(Color.WHITE);
        Rook rook = new Rook(Color.WHITE);
        board.setPiece(0, 4, king);
        board.setPiece(0, 7, rook);
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(0, 4));
        assertThat(moves).contains(new Position(0, 6));
    }

    @Test
    void whiteKingShouldIncludeQueensideCastleWhenAllowed() {
        King king = new King(Color.WHITE);
        Rook rook = new Rook(Color.WHITE);
        board.setPiece(0, 4, king);
        board.setPiece(0, 0, rook);
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(0, 4));
        assertThat(moves).contains(new Position(0, 2));
    }

    @Test
    void whiteKingShouldNotCastleWhenKingHasMoved() {
        King king = new King(Color.WHITE);
        king.setMoved();
        Rook rook = new Rook(Color.WHITE);
        board.setPiece(0, 4, king);
        board.setPiece(0, 7, rook);
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(0, 4));
        assertThat(moves).isNotEmpty();
        assertThat(moves).doesNotContain(new Position(0, 6));
    }

    @Test
    void whiteKingShouldNotCastleWhenNoRook() {
        King king = new King(Color.WHITE);
        board.setPiece(0, 4, king);
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(0, 4));
        assertThat(moves).doesNotContain(new Position(0, 6));
        assertThat(moves).doesNotContain(new Position(0, 2));
    }

    @Test
    void whiteKingShouldNotCastleWhenRookHasMoved() {
        King king = new King(Color.WHITE);
        Rook rook = new Rook(Color.WHITE);
        rook.setMoved();
        board.setPiece(0, 4, king);
        board.setPiece(0, 7, rook);
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(0, 4));
        assertThat(moves).isNotEmpty();
        assertThat(moves).doesNotContain(new Position(0, 6));
    }

    @Test
    void whiteKingShouldNotCastleQueensideWhenRookHasMoved() {
        King king = new King(Color.WHITE);
        Rook rook = new Rook(Color.WHITE);
        rook.setMoved();
        board.setPiece(0, 4, king);
        board.setPiece(0, 0, rook);
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(0, 4));

        assertThat(moves).isNotEmpty();
        assertThat(moves).doesNotContain(new Position(0, 2));
    }

    @Test
    void whiteKingShouldNotCastleWhenSquaresOccupied() {
        King king = new King(Color.WHITE);
        Rook rook = new Rook(Color.WHITE);
        board.setPiece(0, 4, king);
        board.setPiece(0, 7, rook);
        board.setPiece(0, 6, new Bishop(Color.WHITE));
        board.setPiece(0, 0, new Rook(Color.WHITE));
        board.setPiece(0, 1, new Knight(Color.WHITE));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(0, 4));
        assertThat(moves).doesNotContain(new Position(0, 6));
        assertThat(moves).doesNotContain(new Position(0, 2));
    }

    @Test
    void whiteKingShouldNotCastleQueensideWhenCol3Occupied() {
        board.setPiece(0, 4, new King(Color.WHITE));
        board.setPiece(0, 0, new Rook(Color.WHITE));
        board.setPiece(0, 3, new Queen(Color.WHITE));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(0, 4));
        assertThat(moves).isNotEmpty();
        assertThat(moves).doesNotContain(new Position(0, 2));
    }

    @Test
    void whiteKingShouldNotCastleQueensideWhenCol2Occupied() {
        board.setPiece(0, 4, new King(Color.WHITE));
        board.setPiece(0, 0, new Rook(Color.WHITE));
        board.setPiece(0, 2, new Bishop(Color.WHITE));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(0, 4));
        assertThat(moves).isNotEmpty();
        assertThat(moves).doesNotContain(new Position(0, 2));
    }

    @Test
    void blackKingShouldIncludeKingsideCastleWhenAllowed() {
        King king = new King(Color.BLACK);
        Rook rook = new Rook(Color.BLACK);
        board.setPiece(7, 4, king);
        board.setPiece(7, 7, rook);
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(7, 4));
        assertThat(moves).contains(new Position(7, 6));
    }

    @Test
    void blackKingShouldIncludeQueensideCastleWhenAllowed() {
        King king = new King(Color.BLACK);
        Rook rook = new Rook(Color.BLACK);
        board.setPiece(7, 4, king);
        board.setPiece(7, 0, rook);
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(7, 4));
        assertThat(moves).contains(new Position(7, 2));
    }

    @Test
    void blackKingShouldNotCastleWhenRookHasMoved() {
        King king = new King(Color.BLACK);
        Rook kingsideRook = new Rook(Color.BLACK);
        kingsideRook.setMoved();
        Rook queensideRook = new Rook(Color.BLACK);
        queensideRook.setMoved();
        board.setPiece(7, 4, king);
        board.setPiece(7, 7, kingsideRook);
        board.setPiece(7, 0, queensideRook);
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(7, 4));
        assertThat(moves).doesNotContain(new Position(7, 6));
        assertThat(moves).doesNotContain(new Position(7, 2));
    }

    @Test
    void blackKingShouldNotCastleWhenSquaresOccupied() {
        King king = new King(Color.BLACK);
        board.setPiece(7, 4, king);
        board.setPiece(7, 7, new Rook(Color.BLACK));
        board.setPiece(7, 6, new Bishop(Color.BLACK));
        board.setPiece(7, 0, new Rook(Color.BLACK));
        board.setPiece(7, 3, new Queen(Color.BLACK));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(7, 4));
        assertThat(moves).doesNotContain(new Position(7, 6));
        assertThat(moves).doesNotContain(new Position(7, 2));
    }

    @Test
    void blackKingShouldNotCastleQueensideWhenCol2Occupied() {
        board.setPiece(7, 4, new King(Color.BLACK));
        board.setPiece(7, 0, new Rook(Color.BLACK));
        board.setPiece(7, 2, new Bishop(Color.BLACK));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(7, 4));
        assertThat(moves).isNotEmpty();
        assertThat(moves).doesNotContain(new Position(7, 2));
    }

    // --- Pawn ---

    @Test
    void whitePawnShouldMoveForward() {
        board.setPiece(2, 4, new Pawn(Color.WHITE));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(2, 4));
        assertThat(moves).contains(new Position(3, 4));
    }

    @Test
    void whitePawnShouldHaveNoMovesWhenBlocked() {
        board.setPiece(2, 4, new Pawn(Color.WHITE));
        board.setPiece(3, 4, new Pawn(Color.BLACK));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(2, 4));
        assertThat(moves).isEmpty();
    }

    @Test
    void whitePawnShouldDoubleMoveFromStartRow() {
        board.setPiece(1, 4, new Pawn(Color.WHITE));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(1, 4));
        assertThat(moves).contains(new Position(3, 4));
    }

    @Test
    void whitePawnShouldNotDoubleMoveWhenAlreadyMoved() {
        Pawn p = new Pawn(Color.WHITE);
        p.setMoved();
        board.setPiece(1, 4, p);
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(1, 4));
        assertThat(moves).isNotEmpty();
        assertThat(moves).doesNotContain(new Position(3, 4));
    }

    @Test
    void whitePawnShouldNotDoubleMoveWhenFirstSquareBlocked() {
        Pawn p = new Pawn(Color.WHITE);
        board.setPiece(1, 4, p);
        board.setPiece(2, 4, new Pawn(Color.BLACK));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(1, 4));
        assertThat(moves).isEmpty();
    }

    @Test
    void whitePawnShouldNotDoubleMoveWhenSecondSquareBlocked() {
        board.setPiece(1, 4, new Pawn(Color.WHITE));
        board.setPiece(3, 4, new Pawn(Color.BLACK));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(1, 4));
        assertThat(moves).contains(new Position(2, 4));
        assertThat(moves).doesNotContain(new Position(3, 4));
    }

    @Test
    void whitePawnShouldCaptureDiagonally() {
        board.setPiece(2, 4, new Pawn(Color.WHITE));
        board.setPiece(3, 3, new Pawn(Color.BLACK));
        board.setPiece(3, 5, new Pawn(Color.BLACK));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(2, 4));
        assertThat(moves).contains(new Position(3, 3)).contains(new Position(3, 5));
    }

    @Test
    void whitePawnShouldNotCaptureOwnPiece() {
        board.setPiece(2, 4, new Pawn(Color.WHITE));
        board.setPiece(3, 3, new Pawn(Color.WHITE));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(2, 4));
        assertThat(moves).isNotEmpty();
        assertThat(moves).doesNotContain(new Position(3, 3));
    }

    @Test
    void whitePawnShouldNotCaptureOwnPieceOnRightDiagonal() {
        board.setPiece(2, 4, new Pawn(Color.WHITE));
        board.setPiece(3, 5, new Pawn(Color.WHITE));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(2, 4));
        assertThat(moves).isNotEmpty();
        assertThat(moves).doesNotContain(new Position(3, 5));
    }

    @Test
    void whitePawnShouldIncludeEnPassantTarget() {
        board.setPiece(4, 4, new Pawn(Color.WHITE));
        game.setEnPassantTarget(new Position(5, 5));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(4, 4));
        assertThat(moves).contains(new Position(5, 5));
    }

    @Test
    void whitePawnShouldNotIncludeEnPassantWhenNotAdjacent() {
        board.setPiece(4, 4, new Pawn(Color.WHITE));
        game.setEnPassantTarget(new Position(5, 7));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(4, 4));
        assertThat(moves).isNotEmpty();
        assertThat(moves).doesNotContain(new Position(5, 7));
    }

    @Test
    void blackPawnShouldMoveForward() {
        board.setPiece(5, 4, new Pawn(Color.BLACK));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(5, 4));
        assertThat(moves).contains(new Position(4, 4));
    }

    @Test
    void blackPawnShouldDoubleMoveFromStartRow() {
        board.setPiece(6, 4, new Pawn(Color.BLACK));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(6, 4));
        assertThat(moves).contains(new Position(4, 4));
    }

    @Test
    void blackPawnShouldNotDoubleMoveWhenAlreadyMoved() {
        Pawn p = new Pawn(Color.BLACK);
        p.setMoved();
        board.setPiece(6, 4, p);
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(6, 4));
        assertThat(moves).isNotEmpty();
        assertThat(moves).doesNotContain(new Position(4, 4));
    }

    @Test
    void blackPawnShouldCaptureDiagonally() {
        board.setPiece(5, 4, new Pawn(Color.BLACK));
        board.setPiece(4, 3, new Pawn(Color.WHITE));
        board.setPiece(4, 5, new Pawn(Color.WHITE));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(5, 4));
        assertThat(moves).contains(new Position(4, 3)).contains(new Position(4, 5));
    }

    @Test
    void blackPawnShouldNotCaptureOwnPiece() {
        board.setPiece(5, 4, new Pawn(Color.BLACK));
        board.setPiece(4, 5, new Pawn(Color.BLACK));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(5, 4));
        assertThat(moves).isNotEmpty();
        assertThat(moves).doesNotContain(new Position(4, 5));
    }

    @Test
    void blackPawnShouldNotCaptureOwnPieceOnLeftDiagonal() {
        board.setPiece(5, 4, new Pawn(Color.BLACK));
        board.setPiece(4, 3, new Pawn(Color.BLACK));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(5, 4));
        assertThat(moves).isNotEmpty();
        assertThat(moves).doesNotContain(new Position(4, 3));
    }

    @Test
    void blackPawnShouldIncludeEnPassantTarget() {
        board.setPiece(3, 4, new Pawn(Color.BLACK));
        game.setEnPassantTarget(new Position(2, 3));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(3, 4));
        assertThat(moves).contains(new Position(2, 3));
    }

    @Test
    void blackPawnShouldNotIncludeEnPassantWhenNotAdjacent() {
        board.setPiece(3, 4, new Pawn(Color.BLACK));
        game.setEnPassantTarget(new Position(2, 0));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(3, 4));
        assertThat(moves).isNotEmpty();
        assertThat(moves).doesNotContain(new Position(2, 0));
    }

    @Test
    void blackPawnShouldNotMoveWhenBlocked() {
        board.setPiece(5, 4, new Pawn(Color.BLACK));
        board.setPiece(4, 4, new Pawn(Color.WHITE));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(5, 4));
        assertThat(moves).doesNotContain(new Position(4, 4));
        assertThat(moves).doesNotContain(new Position(3, 4));
    }

    @Test
    void blackPawnShouldNotDoubleMoveWhenSecondSquareBlocked() {
        board.setPiece(6, 4, new Pawn(Color.BLACK));
        board.setPiece(4, 4, new Pawn(Color.WHITE));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(6, 4));
        assertThat(moves).contains(new Position(5, 4));
        assertThat(moves).doesNotContain(new Position(4, 4));
    }

    @Test
    void whitePawnAtLeftEdgeShouldNotCaptureOffBoard() {
        board.setPiece(2, 0, new Pawn(Color.WHITE));
        board.setPiece(3, 1, new Pawn(Color.BLACK));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(2, 0));
        assertThat(moves).contains(new Position(3, 1));
        // col -1 would be off-board — verifies no ArrayIndexOutOfBoundsException
    }

    @Test
    void blackPawnAtLeftEdgeShouldNotCaptureOffBoard() {
        board.setPiece(4, 0, new Pawn(Color.BLACK));
        board.setPiece(3, 1, new Pawn(Color.WHITE));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(4, 0));
        assertThat(moves).contains(new Position(3, 1));
    }

    @Test
    void whitePawnAtRow6ShouldNotDoubleMoveEvenIfUnmoved() {
        board.setPiece(6, 4, new Pawn(Color.WHITE));
        List<Position> moves = gen.getPseudoLegalMoves(game, board, new Position(6, 4));
        assertThat(moves).containsExactly(new Position(7, 4));
    }
}
