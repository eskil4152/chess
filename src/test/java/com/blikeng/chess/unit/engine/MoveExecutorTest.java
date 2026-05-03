package com.blikeng.chess.unit.engine;

import com.blikeng.chess.exception.errorTypes.InvalidPromotionException;
import com.blikeng.chess.model.*;
import com.blikeng.chess.model.piece.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.blikeng.chess.engine.MoveExecutor;
import com.blikeng.chess.model.Board;

class MoveExecutorTest {

    private MoveExecutor executor;
    private Game game;
    private Board board;

    @BeforeEach
    void setup() {
        executor = new MoveExecutor();
        game = new Game(UUID.randomUUID(), UUID.randomUUID(), "w", UUID.randomUUID(), "b");
        game.setWhiteKingPosition(new Position(7, 4));
        game.setBlackKingPosition(new Position(0, 4));
        board = game.getBoard();
        clearBoard();
        // Keep kings on board so check detection works
        board.setPiece(7, 4, new King(Color.WHITE));
        board.setPiece(0, 4, new King(Color.BLACK));
    }

    private void clearBoard() {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                board.setPiece(r, c, null);
    }

    // --- Basic validation ---

    @Test
    void shouldReturnNullWhenNoPieceAtFrom() {
        GameStatus result = executor.performMove(game, new Move(new Position(3, 3), new Position(4, 3)), null);
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullWhenWrongColorMoves() {
        board.setPiece(1, 4, new Pawn(Color.BLACK));
        GameStatus result = executor.performMove(game, new Move(new Position(1, 4), new Position(2, 4)), null);
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullForIllegalMove() {
        board.setPiece(6, 4, new Pawn(Color.WHITE));
        GameStatus result = executor.performMove(game, new Move(new Position(6, 4), new Position(5, 5)), null);
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullWhenMoveLeavesKingInCheck() {
        board.setPiece(7, 3, new Rook(Color.WHITE));
        board.setPiece(7, 0, new Rook(Color.BLACK));
        GameStatus result = executor.performMove(game, new Move(new Position(7, 3), new Position(3, 3)), null);
        assertThat(result).isNull();
    }

    // --- Regular moves ---

    @Test
    void pawnForwardMoveShouldReturnOngoingAndMovePiece() {
        board.setPiece(6, 4, new Pawn(Color.WHITE));
        board.setPiece(1, 0, new Pawn(Color.BLACK));
        GameStatus result = executor.performMove(game, new Move(new Position(6, 4), new Position(5, 4)), null);
        assertThat(result).isEqualTo(GameStatus.ONGOING);
        assertThat(board.getPiece(5, 4)).isInstanceOf(Pawn.class);
        assertThat(board.getPiece(6, 4)).isNull();
        assertThat(game.isWhiteTurn()).isFalse();
    }

    @Test
    void moveShouldSetMovedFlag() {
        board.setPiece(6, 4, new Pawn(Color.WHITE));
        board.setPiece(1, 0, new Pawn(Color.BLACK));
        executor.performMove(game, new Move(new Position(6, 4), new Position(5, 4)), null);
        assertThat(board.getPiece(5, 4).hasMoved()).isTrue();
    }

    @Test
    void doublePawnPushShouldSetEnPassantTarget() {
        board.setPiece(6, 4, new Pawn(Color.WHITE));
        board.setPiece(1, 0, new Pawn(Color.BLACK));
        executor.performMove(game, new Move(new Position(6, 4), new Position(4, 4)), null);
        assertThat(game.getEnPassantTarget()).isEqualTo(new Position(5, 4));
    }

    @Test
    void nonDoublePushShouldClearEnPassantTarget() {
        game.setEnPassantTarget(new Position(5, 3));
        board.setPiece(6, 4, new Pawn(Color.WHITE));
        board.setPiece(1, 0, new Pawn(Color.BLACK));
        executor.performMove(game, new Move(new Position(6, 4), new Position(5, 4)), null);
        assertThat(game.getEnPassantTarget()).isNull();
    }

    // --- En passant ---

    @Test
    void whiteEnPassantShouldCaptureBlackPawn() {
        board.setPiece(3, 4, new Pawn(Color.WHITE));
        board.setPiece(3, 5, new Pawn(Color.BLACK));
        board.setPiece(1, 0, new King(Color.BLACK));
        game.setEnPassantTarget(new Position(2, 5));
        game.setBlackKingPosition(new Position(1, 0));

        GameStatus result = executor.performMove(game, new Move(new Position(3, 4), new Position(2, 5)), null);

        assertThat(result).isNotNull();
        assertThat(board.getPiece(3, 5)).isNull();
        assertThat(board.getPiece(2, 5)).isInstanceOf(Pawn.class);
    }

    @Test
    void blackEnPassantShouldCaptureWhitePawn() {
        game.switchTurn();
        board.setPiece(4, 4, new Pawn(Color.BLACK));
        board.setPiece(4, 3, new Pawn(Color.WHITE));
        board.setPiece(6, 0, new King(Color.WHITE));
        game.setEnPassantTarget(new Position(5, 3));
        game.setWhiteKingPosition(new Position(6, 0));

        GameStatus result = executor.performMove(game, new Move(new Position(4, 4), new Position(5, 3)), null);

        assertThat(result).isNotNull();
        assertThat(board.getPiece(4, 3)).isNull();
        assertThat(board.getPiece(5, 3)).isInstanceOf(Pawn.class);
    }

    // --- Pawn promotion ---
    // Use col=2 so promotion square (0,2) isn't blocked by the @BeforeEach black king at (0,4).
    // Use setMoved() on the pawn to avoid the double-push path hitting row -1.

    private Pawn movedWhitePawn() {
        Pawn p = new Pawn(Color.WHITE);
        p.setMoved();
        return p;
    }

    private Pawn movedBlackPawn() {
        Pawn p = new Pawn(Color.BLACK);
        p.setMoved();
        return p;
    }

    @Test
    void whitePawnShouldPromoteToQueen() {
        board.setPiece(1, 2, movedWhitePawn());
        GameStatus result = executor.performMove(game, new Move(new Position(1, 2), new Position(0, 2)), PieceType.QUEEN);
        assertThat(result).isNotNull();
        assertThat(board.getPiece(0, 2)).isInstanceOf(Queen.class).extracting(Piece::getColor).isEqualTo(Color.WHITE);
    }

    @Test
    void whitePawnShouldPromoteToRook() {
        board.setPiece(1, 2, movedWhitePawn());
        executor.performMove(game, new Move(new Position(1, 2), new Position(0, 2)), PieceType.ROOK);
        assertThat(board.getPiece(0, 2)).isInstanceOf(Rook.class);
    }

    @Test
    void whitePawnShouldPromoteToBishop() {
        board.setPiece(1, 2, movedWhitePawn());
        executor.performMove(game, new Move(new Position(1, 2), new Position(0, 2)), PieceType.BISHOP);
        assertThat(board.getPiece(0, 2)).isInstanceOf(Bishop.class);
    }

    @Test
    void whitePawnShouldPromoteToKnight() {
        board.setPiece(1, 2, movedWhitePawn());
        executor.performMove(game, new Move(new Position(1, 2), new Position(0, 2)), PieceType.KNIGHT);
        assertThat(board.getPiece(0, 2)).isInstanceOf(Knight.class);
    }

    @Test
    void whitePawnPromotionWithoutPieceShouldThrow() {
        board.setPiece(1, 2, movedWhitePawn());
        assertThatThrownBy(() ->
                executor.performMove(game, new Move(new Position(1, 2), new Position(0, 2)), null)
        ).isInstanceOf(InvalidPromotionException.class);
    }

    @Test
    void blackPawnShouldPromoteToQueen() {
        game.switchTurn();
        board.setPiece(7, 4, null);
        board.setPiece(7, 0, new King(Color.WHITE));
        game.setWhiteKingPosition(new Position(7, 0));
        board.setPiece(6, 2, movedBlackPawn());
        GameStatus result = executor.performMove(game, new Move(new Position(6, 2), new Position(7, 2)), PieceType.QUEEN);
        assertThat(result).isNotNull();
        assertThat(board.getPiece(7, 2)).isInstanceOf(Queen.class).extracting(Piece::getColor).isEqualTo(Color.BLACK);
    }

    @Test
    void blackPawnShouldPromoteToRook() {
        game.switchTurn();
        board.setPiece(7, 4, null);
        board.setPiece(7, 0, new King(Color.WHITE));
        game.setWhiteKingPosition(new Position(7, 0));
        board.setPiece(6, 2, movedBlackPawn());
        executor.performMove(game, new Move(new Position(6, 2), new Position(7, 2)), PieceType.ROOK);
        assertThat(board.getPiece(7, 2)).isInstanceOf(Rook.class).extracting(Piece::getColor).isEqualTo(Color.BLACK);
    }

    @Test
    void blackPawnShouldPromoteToBishop() {
        game.switchTurn();
        board.setPiece(7, 4, null);
        board.setPiece(7, 0, new King(Color.WHITE));
        game.setWhiteKingPosition(new Position(7, 0));
        board.setPiece(6, 2, movedBlackPawn());
        executor.performMove(game, new Move(new Position(6, 2), new Position(7, 2)), PieceType.BISHOP);
        assertThat(board.getPiece(7, 2)).isInstanceOf(Bishop.class).extracting(Piece::getColor).isEqualTo(Color.BLACK);
    }

    @Test
    void blackPawnShouldPromoteToKnight() {
        game.switchTurn();
        board.setPiece(7, 4, null);
        board.setPiece(7, 0, new King(Color.WHITE));
        game.setWhiteKingPosition(new Position(7, 0));
        board.setPiece(6, 2, movedBlackPawn());
        executor.performMove(game, new Move(new Position(6, 2), new Position(7, 2)), PieceType.KNIGHT);
        assertThat(board.getPiece(7, 2)).isInstanceOf(Knight.class).extracting(Piece::getColor).isEqualTo(Color.BLACK);
    }

    @Test
    void blackPawnPromotionWithoutPieceShouldThrow() {
        game.switchTurn();
        board.setPiece(7, 4, null);
        board.setPiece(7, 0, new King(Color.WHITE));
        game.setWhiteKingPosition(new Position(7, 0));
        board.setPiece(6, 2, movedBlackPawn());
        assertThatThrownBy(() ->
                executor.performMove(game, new Move(new Position(6, 2), new Position(7, 2)), null)
        ).isInstanceOf(InvalidPromotionException.class);
    }

    @Test
    void promotionToKingShouldThrow() {
        board.setPiece(1, 2, movedWhitePawn());
        assertThatThrownBy(() ->
                executor.performMove(game, new Move(new Position(1, 2), new Position(0, 2)), PieceType.KING)
        ).isInstanceOf(InvalidPromotionException.class);
    }

    // --- Castling ---

    @Test
    void whiteKingShouldCastleKingside() {
        King king = new King(Color.WHITE);
        Rook rook = new Rook(Color.WHITE);
        board.setPiece(7, 4, king);
        board.setPiece(7, 7, rook);
        board.setPiece(1, 0, new Pawn(Color.BLACK));

        GameStatus result = executor.performMove(game, new Move(new Position(7, 4), new Position(7, 6)), null);

        assertThat(result).isNotNull();
        assertThat(board.getPiece(7, 6)).isInstanceOf(King.class);
        assertThat(board.getPiece(7, 5)).isInstanceOf(Rook.class);
        assertThat(board.getPiece(7, 7)).isNull();
        assertThat(board.getPiece(7, 4)).isNull();
    }

    @Test
    void whiteKingShouldCastleQueenside() {
        King king = new King(Color.WHITE);
        Rook rook = new Rook(Color.WHITE);
        board.setPiece(7, 4, king);
        board.setPiece(7, 0, rook);
        board.setPiece(1, 0, new Pawn(Color.BLACK));

        executor.performMove(game, new Move(new Position(7, 4), new Position(7, 2)), null);

        assertThat(board.getPiece(7, 2)).isInstanceOf(King.class);
        assertThat(board.getPiece(7, 3)).isInstanceOf(Rook.class);
        assertThat(board.getPiece(7, 0)).isNull();
    }

    @Test
    void blackKingShouldCastleKingside() {
        game.switchTurn();
        board.setPiece(7, 4, null);
        board.setPiece(7, 0, new King(Color.WHITE));
        game.setWhiteKingPosition(new Position(7, 0));
        board.setPiece(0, 4, new King(Color.BLACK));
        board.setPiece(0, 7, new Rook(Color.BLACK));
        board.setPiece(6, 0, new Pawn(Color.WHITE));

        GameStatus result = executor.performMove(game, new Move(new Position(0, 4), new Position(0, 6)), null);

        assertThat(result).isNotNull();
        assertThat(board.getPiece(0, 6)).isInstanceOf(King.class);
        assertThat(board.getPiece(0, 5)).isInstanceOf(Rook.class);
        assertThat(board.getPiece(0, 7)).isNull();
        assertThat(board.getPiece(0, 4)).isNull();
    }

    @Test
    void castlingShouldLeaveRookUnmovedWhenTransitIsAttackedByIndirectPiece() {
        // Knight at (6,3) attacks (7,5) — the transit square — but not (7,4) or (7,6).
        // So castling appears in pseudo-legal moves and kingLeftInCheck passes,
        // but canCastle's second attacker check denies the rook relocation.
        board.setPiece(7, 4, new King(Color.WHITE));
        board.setPiece(7, 7, new Rook(Color.WHITE));
        board.setPiece(6, 3, new Knight(Color.BLACK));

        GameStatus result = executor.performMove(game, new Move(new Position(7, 4), new Position(7, 6)), null);

        assertThat(result).isNotNull();
        assertThat(board.getPiece(7, 6)).isInstanceOf(King.class);
        assertThat(board.getPiece(7, 7)).isInstanceOf(Rook.class);
        assertThat(board.getPiece(7, 5)).isNull();
    }

    @Test
    void castlingShouldLeaveRookUnmovedWhenKingsSourceSquareIsAttacked() {
        // Rook at (6,4) attacks king's source square (7,4) along column 4 but not (7,6).
        // canCastle's first attacker check short-circuits to false, skipping the rook move.
        board.setPiece(7, 4, new King(Color.WHITE));
        board.setPiece(7, 7, new Rook(Color.WHITE));
        board.setPiece(6, 4, new Rook(Color.BLACK));

        GameStatus result = executor.performMove(game, new Move(new Position(7, 4), new Position(7, 6)), null);

        assertThat(result).isNotNull();
        assertThat(board.getPiece(7, 6)).isInstanceOf(King.class);
        assertThat(board.getPiece(7, 7)).isInstanceOf(Rook.class);
        assertThat(board.getPiece(7, 5)).isNull();
    }

    @Test
    void castlingShouldNotOccurWhenTransitSquareIsAttacked() {
        King king = new King(Color.WHITE);
        Rook rook = new Rook(Color.WHITE);
        board.setPiece(7, 4, king);
        board.setPiece(7, 7, rook);
        board.setPiece(7, 5, new Rook(Color.BLACK));

        GameStatus result = executor.performMove(game, new Move(new Position(7, 4), new Position(7, 6)), null);
        assertThat(result).isNull();
    }

    @Test
    void castlingShouldNotOccurWhenKingIsInCheck() {
        King king = new King(Color.WHITE);
        Rook rook = new Rook(Color.WHITE);
        board.setPiece(7, 4, king);
        board.setPiece(7, 7, rook);
        board.setPiece(7, 1, new Rook(Color.BLACK));
        game.setWhiteKingPosition(new Position(7, 4));

        GameStatus result = executor.performMove(game, new Move(new Position(7, 4), new Position(7, 6)), null);
        assertThat(result).isNull();
    }

    // --- King position tracking ---

    @Test
    void whiteKingMoveShouldUpdateKingPosition() {
        board.setPiece(7, 4, new King(Color.WHITE));
        board.setPiece(1, 0, new Pawn(Color.BLACK));
        executor.performMove(game, new Move(new Position(7, 4), new Position(7, 3)), null);
        assertThat(game.getWhiteKingPosition()).isEqualTo(new Position(7, 3));
    }

    @Test
    void blackKingMoveShouldUpdateKingPosition() {
        game.switchTurn();
        board.setPiece(0, 4, new King(Color.BLACK));
        board.setPiece(6, 0, new Pawn(Color.WHITE));
        executor.performMove(game, new Move(new Position(0, 4), new Position(0, 3)), null);
        assertThat(game.getBlackKingPosition()).isEqualTo(new Position(0, 3));
    }

    // --- Game over: checkmate and stalemate ---

    @Test
    void checkmatedBlackKingShouldReturnWhiteWin() {
        board.setPiece(7, 4, null);
        board.setPiece(0, 4, null);
        board.setPiece(3, 6, new Queen(Color.WHITE));
        board.setPiece(2, 5, new King(Color.WHITE));
        board.setPiece(0, 7, new King(Color.BLACK));
        game.setWhiteKingPosition(new Position(2, 5));
        game.setBlackKingPosition(new Position(0, 7));

        GameStatus result = executor.performMove(game, new Move(new Position(3, 6), new Position(1, 6)), null);
        assertThat(result).isEqualTo(GameStatus.WHITE_WIN);
    }

    @Test
    void checkmatedWhiteKingShouldReturnBlackWin() {
        board.setPiece(7, 4, null);
        board.setPiece(0, 4, null);
        board.setPiece(2, 6, new Queen(Color.BLACK));
        board.setPiece(7, 0, new Rook(Color.BLACK));
        board.setPiece(0, 0, new King(Color.BLACK));
        board.setPiece(7, 7, new King(Color.WHITE));
        game.setBlackKingPosition(new Position(0, 0));
        game.setWhiteKingPosition(new Position(7, 7));
        game.switchTurn();

        GameStatus result = executor.performMove(game, new Move(new Position(2, 6), new Position(7, 6)), null);
        assertThat(result).isEqualTo(GameStatus.BLACK_WIN);
    }

    @Test
    void stalemateShouldReturnDraw() {
        board.setPiece(7, 4, null);
        board.setPiece(0, 4, null);
        board.setPiece(4, 1, new Queen(Color.WHITE));
        board.setPiece(2, 3, new King(Color.WHITE));
        board.setPiece(0, 2, new King(Color.BLACK));
        game.setWhiteKingPosition(new Position(2, 3));
        game.setBlackKingPosition(new Position(0, 2));

        GameStatus result = executor.performMove(game, new Move(new Position(4, 1), new Position(2, 1)), null);
        assertThat(result).isEqualTo(GameStatus.DRAW);
    }
}
