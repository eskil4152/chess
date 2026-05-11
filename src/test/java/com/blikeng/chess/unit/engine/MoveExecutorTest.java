package com.blikeng.chess.unit.engine;

import com.blikeng.chess.engine.MoveExecutor;
import com.blikeng.chess.exception.types.InvalidPromotionException;
import com.blikeng.chess.model.*;
import com.blikeng.chess.model.piece.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoveExecutorTest {

    private MoveExecutor executor;
    private Game game;
    private Board board;

    @BeforeEach
    void setup() {
        executor = new MoveExecutor();
        game = new Game(UUID.randomUUID(), UUID.randomUUID(), "w", UUID.randomUUID(), "b", 800, 800);
        game.setWhiteKingPosition(new Position(0, 4));
        game.setBlackKingPosition(new Position(7, 4));
        board = game.getBoard();
        clearBoard();
        // Keep kings on board so check detection works
        board.setPiece(0, 4, new King(Color.WHITE));
        board.setPiece(7, 4, new King(Color.BLACK));
    }

    private void clearBoard() {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                board.setPiece(r, c, null);
    }

    // --- Basic validation ---

    @Test
    void shouldReturnNullWhenNoPieceAtFrom() {
        GameStatus result = executor.performMove(game, new Move(new Position(3, 3), new Position(4, 3), null));
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullWhenWrongColorMoves() {
        board.setPiece(6, 4, new Pawn(Color.BLACK));
        GameStatus result = executor.performMove(game, new Move(new Position(6, 4), new Position(5, 4), null));
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullForIllegalMove() {
        board.setPiece(1, 4, new Pawn(Color.WHITE));
        GameStatus result = executor.performMove(game, new Move(new Position(1, 4), new Position(2, 5), null));
        assertThat(result).isNull();
    }

    @Test
    void shouldReturnNullWhenMoveLeavesKingInCheck() {
        board.setPiece(0, 3, new Rook(Color.WHITE));
        board.setPiece(0, 0, new Rook(Color.BLACK));
        GameStatus result = executor.performMove(game, new Move(new Position(0, 3), new Position(3, 3), null));
        assertThat(result).isNull();
    }

    // --- Regular moves ---

    @Test
    void pawnForwardMoveShouldReturnOngoingAndMovePiece() {
        board.setPiece(1, 4, new Pawn(Color.WHITE));
        board.setPiece(6, 0, new Pawn(Color.BLACK));
        GameStatus result = executor.performMove(game, new Move(new Position(1, 4), new Position(2, 4), null));
        assertThat(result).isEqualTo(GameStatus.ONGOING);
        assertThat(board.getPiece(2, 4)).isInstanceOf(Pawn.class);
        assertThat(board.getPiece(1, 4)).isNull();
        assertThat(game.isWhiteTurn()).isFalse();
    }

    @Test
    void moveShouldSetMovedFlag() {
        board.setPiece(1, 4, new Pawn(Color.WHITE));
        board.setPiece(6, 0, new Pawn(Color.BLACK));
        executor.performMove(game, new Move(new Position(1, 4), new Position(2, 4), null));
        assertThat(board.getPiece(2, 4).hasMoved()).isTrue();
    }

    @Test
    void doublePawnPushShouldSetEnPassantTarget() {
        board.setPiece(1, 4, new Pawn(Color.WHITE));
        board.setPiece(6, 0, new Pawn(Color.BLACK));
        executor.performMove(game, new Move(new Position(1, 4), new Position(3, 4), null));
        assertThat(game.getEnPassantTarget()).isEqualTo(new Position(2, 4));
    }

    @Test
    void doublePawnPushWithAdjacentBlackPawnShouldEvaluateEnPassantAsLegalMove() {
        board.setPiece(1, 4, new Pawn(Color.WHITE));
        board.setPiece(3, 5, new Pawn(Color.BLACK));
        // Block the black pawn's forward square so its only pseudo-legal move is the ep capture,
        // forcing isGameOver to evaluate epMove=true before finding any other legal move.
        board.setPiece(2, 5, movedWhitePawn());
        GameStatus result = executor.performMove(game, new Move(new Position(1, 4), new Position(3, 4), null));
        assertThat(result).isEqualTo(GameStatus.ONGOING);
        assertThat(game.getEnPassantTarget()).isEqualTo(new Position(2, 4));
    }

    @Test
    void nonDoublePushShouldClearEnPassantTarget() {
        game.setEnPassantTarget(new Position(2, 3));
        board.setPiece(1, 4, new Pawn(Color.WHITE));
        board.setPiece(6, 0, new Pawn(Color.BLACK));
        executor.performMove(game, new Move(new Position(1, 4), new Position(2, 4), null));
        assertThat(game.getEnPassantTarget()).isNull();
    }

    // --- En passant ---

    @Test
    void whiteEnPassantShouldCaptureBlackPawn() {
        board.setPiece(4, 4, new Pawn(Color.WHITE));
        board.setPiece(4, 5, new Pawn(Color.BLACK));
        board.setPiece(6, 0, new King(Color.BLACK));
        game.setEnPassantTarget(new Position(5, 5));
        game.setBlackKingPosition(new Position(6, 0));

        GameStatus result = executor.performMove(game, new Move(new Position(4, 4), new Position(5, 5), null));

        assertThat(result).isNotNull();
        assertThat(board.getPiece(4, 5)).isNull();
        assertThat(board.getPiece(5, 5)).isInstanceOf(Pawn.class);
    }

    @Test
    void blackEnPassantShouldCaptureWhitePawn() {
        game.switchTurn();
        board.setPiece(3, 4, new Pawn(Color.BLACK));
        board.setPiece(3, 3, new Pawn(Color.WHITE));
        board.setPiece(1, 0, new King(Color.WHITE));
        game.setEnPassantTarget(new Position(2, 3));
        game.setWhiteKingPosition(new Position(1, 0));

        GameStatus result = executor.performMove(game, new Move(new Position(3, 4), new Position(2, 3), null));

        assertThat(result).isNotNull();
        assertThat(board.getPiece(3, 3)).isNull();
        assertThat(board.getPiece(2, 3)).isInstanceOf(Pawn.class);
    }

    // --- Pawn promotion ---
    // Use col=2 so white promotion square (7,2) isn't blocked by the @BeforeEach black king at (7,4).
    // Use setMoved() on the pawn to avoid the double-push path going out of bounds.

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
        board.setPiece(6, 2, movedWhitePawn());
        GameStatus result = executor.performMove(game, new Move(new Position(6, 2), new Position(7, 2), PieceType.QUEEN));
        assertThat(result).isNotNull();
        assertThat(board.getPiece(7, 2)).isInstanceOf(Queen.class).extracting(Piece::getColor).isEqualTo(Color.WHITE);
    }

    @Test
    void whitePawnShouldPromoteToRook() {
        board.setPiece(6, 2, movedWhitePawn());
        executor.performMove(game, new Move(new Position(6, 2), new Position(7, 2), PieceType.ROOK));
        assertThat(board.getPiece(7, 2)).isInstanceOf(Rook.class);
    }

    @Test
    void whitePawnShouldPromoteToBishop() {
        board.setPiece(6, 2, movedWhitePawn());
        executor.performMove(game, new Move(new Position(6, 2), new Position(7, 2), PieceType.BISHOP));
        assertThat(board.getPiece(7, 2)).isInstanceOf(Bishop.class);
    }

    @Test
    void whitePawnShouldPromoteToKnight() {
        board.setPiece(6, 2, movedWhitePawn());
        executor.performMove(game, new Move(new Position(6, 2), new Position(7, 2), PieceType.KNIGHT));
        assertThat(board.getPiece(7, 2)).isInstanceOf(Knight.class);
    }

    @Test
    void whitePawnPromotionWithoutPieceShouldThrow() {
        board.setPiece(6, 2, movedWhitePawn());
        assertThatThrownBy(() ->
                executor.performMove(game, new Move(new Position(6, 2), new Position(7, 2), null))
        ).isInstanceOf(InvalidPromotionException.class);
    }

    @Test
    void blackPawnShouldPromoteToQueen() {
        game.switchTurn();
        board.setPiece(0, 4, null);
        board.setPiece(0, 6, new King(Color.WHITE));
        game.setWhiteKingPosition(new Position(0, 6));
        board.setPiece(1, 2, movedBlackPawn());
        GameStatus result = executor.performMove(game, new Move(new Position(1, 2), new Position(0, 2), PieceType.QUEEN));
        assertThat(result).isNotNull();
        assertThat(board.getPiece(0, 2)).isInstanceOf(Queen.class).extracting(Piece::getColor).isEqualTo(Color.BLACK);
    }

    @Test
    void blackPawnShouldPromoteToRook() {
        game.switchTurn();
        board.setPiece(0, 4, null);
        board.setPiece(0, 6, new King(Color.WHITE));
        game.setWhiteKingPosition(new Position(0, 6));
        board.setPiece(1, 2, movedBlackPawn());
        executor.performMove(game, new Move(new Position(1, 2), new Position(0, 2), PieceType.ROOK));
        assertThat(board.getPiece(0, 2)).isInstanceOf(Rook.class).extracting(Piece::getColor).isEqualTo(Color.BLACK);
    }

    @Test
    void blackPawnShouldPromoteToBishop() {
        game.switchTurn();
        board.setPiece(0, 4, null);
        board.setPiece(0, 6, new King(Color.WHITE));
        game.setWhiteKingPosition(new Position(0, 6));
        board.setPiece(1, 2, movedBlackPawn());
        executor.performMove(game, new Move(new Position(1, 2), new Position(0, 2), PieceType.BISHOP));
        assertThat(board.getPiece(0, 2)).isInstanceOf(Bishop.class).extracting(Piece::getColor).isEqualTo(Color.BLACK);
    }

    @Test
    void blackPawnShouldPromoteToKnight() {
        game.switchTurn();
        board.setPiece(0, 4, null);
        board.setPiece(0, 6, new King(Color.WHITE));
        game.setWhiteKingPosition(new Position(0, 6));
        board.setPiece(1, 2, movedBlackPawn());
        executor.performMove(game, new Move(new Position(1, 2), new Position(0, 2), PieceType.KNIGHT));
        assertThat(board.getPiece(0, 2)).isInstanceOf(Knight.class).extracting(Piece::getColor).isEqualTo(Color.BLACK);
    }

    @Test
    void blackPawnPromotionWithoutPieceShouldThrow() {
        game.switchTurn();
        board.setPiece(0, 4, null);
        board.setPiece(0, 6, new King(Color.WHITE));
        game.setWhiteKingPosition(new Position(0, 6));
        board.setPiece(1, 2, movedBlackPawn());
        assertThatThrownBy(() ->
                executor.performMove(game, new Move(new Position(1, 2), new Position(0, 2), null))
        ).isInstanceOf(InvalidPromotionException.class);
    }

    @Test
    void promotionToKingShouldThrow() {
        board.setPiece(6, 2, movedWhitePawn());
        assertThatThrownBy(() ->
                executor.performMove(game, new Move(new Position(6, 2), new Position(7, 2), PieceType.KING))
        ).isInstanceOf(InvalidPromotionException.class);
    }

    // --- Castling ---

    @Test
    void whiteKingShouldCastleKingside() {
        King king = new King(Color.WHITE);
        Rook rook = new Rook(Color.WHITE);
        board.setPiece(0, 4, king);
        board.setPiece(0, 7, rook);
        board.setPiece(6, 0, new Pawn(Color.BLACK));

        GameStatus result = executor.performMove(game, new Move(new Position(0, 4), new Position(0, 6), null));

        assertThat(result).isNotNull();
        assertThat(board.getPiece(0, 6)).isInstanceOf(King.class);
        assertThat(board.getPiece(0, 5)).isInstanceOf(Rook.class);
        assertThat(board.getPiece(0, 7)).isNull();
        assertThat(board.getPiece(0, 4)).isNull();
    }

    @Test
    void whiteKingShouldCastleQueenside() {
        King king = new King(Color.WHITE);
        Rook rook = new Rook(Color.WHITE);
        board.setPiece(0, 4, king);
        board.setPiece(0, 0, rook);
        board.setPiece(6, 0, new Pawn(Color.BLACK));

        executor.performMove(game, new Move(new Position(0, 4), new Position(0, 2), null));

        assertThat(board.getPiece(0, 2)).isInstanceOf(King.class);
        assertThat(board.getPiece(0, 3)).isInstanceOf(Rook.class);
        assertThat(board.getPiece(0, 0)).isNull();
    }

    @Test
    void blackKingShouldCastleKingside() {
        game.switchTurn();
        board.setPiece(0, 4, null);
        board.setPiece(0, 0, new King(Color.WHITE));
        game.setWhiteKingPosition(new Position(0, 0));
        board.setPiece(7, 4, new King(Color.BLACK));
        board.setPiece(7, 7, new Rook(Color.BLACK));
        board.setPiece(1, 0, new Pawn(Color.WHITE));

        GameStatus result = executor.performMove(game, new Move(new Position(7, 4), new Position(7, 6), null));

        assertThat(result).isNotNull();
        assertThat(board.getPiece(7, 6)).isInstanceOf(King.class);
        assertThat(board.getPiece(7, 5)).isInstanceOf(Rook.class);
        assertThat(board.getPiece(7, 7)).isNull();
        assertThat(board.getPiece(7, 4)).isNull();
    }

    @Test
    void castlingShouldLeaveRookUnmovedWhenTransitIsAttackedByIndirectPiece() {
        // Knight at (1,3) attacks (0,5) — the transit square — but not (0,4) or (0,6).
        // So castling appears in pseudo-legal moves and kingLeftInCheck passes,
        // but canCastle's second attacker check denies the rook relocation.
        board.setPiece(0, 4, new King(Color.WHITE));
        board.setPiece(0, 7, new Rook(Color.WHITE));
        board.setPiece(1, 3, new Knight(Color.BLACK));

        GameStatus result = executor.performMove(game, new Move(new Position(0, 4), new Position(0, 6), null));

        assertThat(result).isNotNull();
        assertThat(board.getPiece(0, 6)).isInstanceOf(King.class);
        assertThat(board.getPiece(0, 7)).isInstanceOf(Rook.class);
        assertThat(board.getPiece(0, 5)).isNull();
    }

    @Test
    void castlingShouldLeaveRookUnmovedWhenKingsSourceSquareIsAttacked() {
        // Rook at (1,4) attacks king's source square (0,4) along column 4 but not (0,6).
        // canCastle's first attacker check short-circuits to false, skipping the rook move.
        board.setPiece(0, 4, new King(Color.WHITE));
        board.setPiece(0, 7, new Rook(Color.WHITE));
        board.setPiece(1, 4, new Rook(Color.BLACK));

        GameStatus result = executor.performMove(game, new Move(new Position(0, 4), new Position(0, 6), null));

        assertThat(result).isNotNull();
        assertThat(board.getPiece(0, 6)).isInstanceOf(King.class);
        assertThat(board.getPiece(0, 7)).isInstanceOf(Rook.class);
        assertThat(board.getPiece(0, 5)).isNull();
    }

    @Test
    void castlingShouldNotOccurWhenTransitSquareIsAttacked() {
        King king = new King(Color.WHITE);
        Rook rook = new Rook(Color.WHITE);
        board.setPiece(0, 4, king);
        board.setPiece(0, 7, rook);
        board.setPiece(0, 5, new Rook(Color.BLACK));

        GameStatus result = executor.performMove(game, new Move(new Position(0, 4), new Position(0, 6), null));
        assertThat(result).isNull();
    }

    @Test
    void castlingShouldNotOccurWhenKingIsInCheck() {
        King king = new King(Color.WHITE);
        Rook rook = new Rook(Color.WHITE);
        board.setPiece(0, 4, king);
        board.setPiece(0, 7, rook);
        board.setPiece(0, 1, new Rook(Color.BLACK));
        game.setWhiteKingPosition(new Position(0, 4));

        GameStatus result = executor.performMove(game, new Move(new Position(0, 4), new Position(0, 6), null));
        assertThat(result).isNull();
    }

    // --- King position tracking ---

    @Test
    void whiteKingMoveShouldUpdateKingPosition() {
        board.setPiece(0, 4, new King(Color.WHITE));
        board.setPiece(6, 0, new Pawn(Color.BLACK));
        executor.performMove(game, new Move(new Position(0, 4), new Position(0, 3), null));
        assertThat(game.getWhiteKingPosition()).isEqualTo(new Position(0, 3));
    }

    @Test
    void blackKingMoveShouldUpdateKingPosition() {
        game.switchTurn();
        board.setPiece(7, 4, new King(Color.BLACK));
        board.setPiece(1, 0, new Pawn(Color.WHITE));
        executor.performMove(game, new Move(new Position(7, 4), new Position(7, 3), null));
        assertThat(game.getBlackKingPosition()).isEqualTo(new Position(7, 3));
    }

    // --- Game over: checkmate and stalemate ---

    @Test
    void checkmatedBlackKingShouldReturnWhiteWin() {
        board.setPiece(0, 4, null);
        board.setPiece(7, 4, null);
        board.setPiece(4, 6, new Queen(Color.WHITE));
        board.setPiece(5, 5, new King(Color.WHITE));
        board.setPiece(7, 7, new King(Color.BLACK));
        game.setWhiteKingPosition(new Position(5, 5));
        game.setBlackKingPosition(new Position(7, 7));

        GameStatus result = executor.performMove(game, new Move(new Position(4, 6), new Position(6, 6), null));
        assertThat(result).isEqualTo(GameStatus.WHITE_WIN);
    }

    @Test
    void checkmatedWhiteKingShouldReturnBlackWin() {
        board.setPiece(0, 4, null);
        board.setPiece(7, 4, null);
        board.setPiece(5, 6, new Queen(Color.BLACK));
        board.setPiece(0, 0, new Rook(Color.BLACK));
        board.setPiece(7, 0, new King(Color.BLACK));
        board.setPiece(0, 7, new King(Color.WHITE));
        game.setBlackKingPosition(new Position(7, 0));
        game.setWhiteKingPosition(new Position(0, 7));
        game.switchTurn();

        GameStatus result = executor.performMove(game, new Move(new Position(5, 6), new Position(0, 6), null));
        assertThat(result).isEqualTo(GameStatus.BLACK_WIN);
    }

    @Test
    void stalemateShouldReturnDraw() {
        board.setPiece(0, 4, null);
        board.setPiece(7, 4, null);
        board.setPiece(3, 1, new Queen(Color.WHITE));
        board.setPiece(5, 3, new King(Color.WHITE));
        board.setPiece(7, 2, new King(Color.BLACK));
        game.setWhiteKingPosition(new Position(5, 3));
        game.setBlackKingPosition(new Position(7, 2));

        GameStatus result = executor.performMove(game, new Move(new Position(3, 1), new Position(5, 1), null));
        assertThat(result).isEqualTo(GameStatus.DRAW);
    }

    // --- 50-move rule ---

    @Test
    void fiftyMoveRuleShouldTriggerDrawAt100HalfMoves() {
        board.setPiece(0, 4, null);
        board.setPiece(7, 4, null);
        board.setPiece(0, 0, new King(Color.WHITE));
        board.setPiece(7, 0, new King(Color.BLACK));
        game.setWhiteKingPosition(new Position(0, 0));
        game.setBlackKingPosition(new Position(7, 0));
        board.setPiece(3, 3, new Knight(Color.WHITE));
        game.setHalfMoveClock(99);

        GameStatus result = executor.performMove(game, new Move(new Position(3, 3), new Position(5, 4), null));
        assertThat(result).isEqualTo(GameStatus.DRAW);
        assertThat(game.getEndedBy()).isEqualTo(EndedBy.FIFTY_MOVE_RULE);
    }

    @Test
    void fiftyMoveRuleShouldNotTriggerAt99HalfMoves() {
        board.setPiece(0, 4, null);
        board.setPiece(7, 4, null);
        board.setPiece(0, 0, new King(Color.WHITE));
        board.setPiece(7, 0, new King(Color.BLACK));
        game.setWhiteKingPosition(new Position(0, 0));
        game.setBlackKingPosition(new Position(7, 0));
        board.setPiece(3, 3, new Rook(Color.WHITE));
        game.setHalfMoveClock(98);

        GameStatus result = executor.performMove(game, new Move(new Position(3, 3), new Position(3, 5), null));
        assertThat(result).isEqualTo(GameStatus.ONGOING);
    }

    @Test
    void halfMoveClockShouldIncrementOnKnightMove() {
        board.setPiece(0, 4, null);
        board.setPiece(7, 4, null);
        board.setPiece(0, 0, new King(Color.WHITE));
        board.setPiece(7, 0, new King(Color.BLACK));
        game.setWhiteKingPosition(new Position(0, 0));
        game.setBlackKingPosition(new Position(7, 0));
        board.setPiece(3, 3, new Knight(Color.WHITE));

        executor.performMove(game, new Move(new Position(3, 3), new Position(5, 4), null));
        assertThat(game.getHalfMoveClock()).isEqualTo(1);
    }

    @Test
    void halfMoveClockShouldResetOnPawnMove() {
        board.setPiece(0, 4, null);
        board.setPiece(7, 4, null);
        board.setPiece(0, 0, new King(Color.WHITE));
        board.setPiece(7, 0, new King(Color.BLACK));
        game.setWhiteKingPosition(new Position(0, 0));
        game.setBlackKingPosition(new Position(7, 0));
        board.setPiece(1, 3, new Pawn(Color.WHITE));
        game.setHalfMoveClock(50);

        executor.performMove(game, new Move(new Position(1, 3), new Position(2, 3), null));
        assertThat(game.getHalfMoveClock()).isZero();
    }

    @Test
    void halfMoveClockShouldResetOnCapture() {
        board.setPiece(0, 4, null);
        board.setPiece(7, 4, null);
        board.setPiece(0, 0, new King(Color.WHITE));
        board.setPiece(7, 0, new King(Color.BLACK));
        game.setWhiteKingPosition(new Position(0, 0));
        game.setBlackKingPosition(new Position(7, 0));
        board.setPiece(3, 3, new Knight(Color.WHITE));
        board.setPiece(5, 4, new Knight(Color.BLACK));
        game.setHalfMoveClock(50);

        executor.performMove(game, new Move(new Position(3, 3), new Position(5, 4), null));
        assertThat(game.getHalfMoveClock()).isZero();
    }

    @Test
    void pawnMoveAtClock99ShouldNotTriggerFiftyMoveRule() {
        board.setPiece(0, 4, null);
        board.setPiece(7, 4, null);
        board.setPiece(0, 0, new King(Color.WHITE));
        board.setPiece(7, 0, new King(Color.BLACK));
        game.setWhiteKingPosition(new Position(0, 0));
        game.setBlackKingPosition(new Position(7, 0));
        board.setPiece(1, 3, new Pawn(Color.WHITE));
        game.setHalfMoveClock(99);

        GameStatus result = executor.performMove(game, new Move(new Position(1, 3), new Position(2, 3), null));
        assertThat(result).isEqualTo(GameStatus.ONGOING);
        assertThat(game.getHalfMoveClock()).isZero();
    }

    // --- Threefold repetition ---
    // Kings in corners, rooks bounce back and forth → K+R vs K+R is never insufficient material.
    // White rook bounces (2,0)↔(2,1), black rook bounces (5,7)↔(5,6).
    // The position after white's rook move first recurs on move 5 and triggers on move 9.

    private void setupRepetitionBoard() {
        board.setPiece(0, 4, null);
        board.setPiece(7, 4, null);
        board.setPiece(0, 0, new King(Color.WHITE));
        board.setPiece(7, 7, new King(Color.BLACK));
        game.setWhiteKingPosition(new Position(0, 0));
        game.setBlackKingPosition(new Position(7, 7));
        board.setPiece(2, 0, new Rook(Color.WHITE));
        board.setPiece(5, 7, new Rook(Color.BLACK));
    }

    @Test
    void positionRepeatedThreeTimesShouldReturnDraw() {
        setupRepetitionBoard();
        for (int i = 0; i < 2; i++) {
            executor.performMove(game, new Move(new Position(2, 0), new Position(2, 1), null));
            executor.performMove(game, new Move(new Position(5, 7), new Position(5, 6), null));
            executor.performMove(game, new Move(new Position(2, 1), new Position(2, 0), null));
            executor.performMove(game, new Move(new Position(5, 6), new Position(5, 7), null));
        }
        GameStatus result = executor.performMove(game, new Move(new Position(2, 0), new Position(2, 1), null));
        assertThat(result).isEqualTo(GameStatus.DRAW);
        assertThat(game.getEndedBy()).isEqualTo(EndedBy.REPETITION);
    }

    @Test
    void positionRepeatedTwiceShouldNotReturnDraw() {
        setupRepetitionBoard();
        executor.performMove(game, new Move(new Position(2, 0), new Position(2, 1), null));
        executor.performMove(game, new Move(new Position(5, 7), new Position(5, 6), null));
        executor.performMove(game, new Move(new Position(2, 1), new Position(2, 0), null));
        executor.performMove(game, new Move(new Position(5, 6), new Position(5, 7), null));
        GameStatus result = executor.performMove(game, new Move(new Position(2, 0), new Position(2, 1), null));
        assertThat(result).isEqualTo(GameStatus.ONGOING);
    }

    // --- Insufficient material ---
    // Each test sets up the exact material and makes any valid move.
    // Kings in corners far apart; no rooks at castling squares, so castling rights are always false.

    @Test
    void kingsAloneShouldReturnDraw() {
        board.setPiece(0, 4, null);
        board.setPiece(7, 4, null);
        board.setPiece(0, 0, new King(Color.WHITE));
        board.setPiece(7, 7, new King(Color.BLACK));
        game.setWhiteKingPosition(new Position(0, 0));
        game.setBlackKingPosition(new Position(7, 7));

        GameStatus result = executor.performMove(game, new Move(new Position(0, 0), new Position(1, 0), null));
        assertThat(result).isEqualTo(GameStatus.DRAW);
        assertThat(game.getEndedBy()).isEqualTo(EndedBy.INSUFFICIENT_MATERIAL);
    }

    @Test
    void kingAndBishopVsKingShouldReturnDraw() {
        board.setPiece(0, 4, null);
        board.setPiece(7, 4, null);
        board.setPiece(0, 0, new King(Color.WHITE));
        board.setPiece(7, 7, new King(Color.BLACK));
        board.setPiece(4, 4, new Bishop(Color.WHITE));
        game.setWhiteKingPosition(new Position(0, 0));
        game.setBlackKingPosition(new Position(7, 7));

        GameStatus result = executor.performMove(game, new Move(new Position(4, 4), new Position(5, 3), null));
        assertThat(result).isEqualTo(GameStatus.DRAW);
        assertThat(game.getEndedBy()).isEqualTo(EndedBy.INSUFFICIENT_MATERIAL);
    }

    @Test
    void kingAndKnightVsKingShouldReturnDraw() {
        board.setPiece(0, 4, null);
        board.setPiece(7, 4, null);
        board.setPiece(0, 0, new King(Color.WHITE));
        board.setPiece(7, 7, new King(Color.BLACK));
        board.setPiece(3, 3, new Knight(Color.WHITE));
        game.setWhiteKingPosition(new Position(0, 0));
        game.setBlackKingPosition(new Position(7, 7));

        GameStatus result = executor.performMove(game, new Move(new Position(3, 3), new Position(5, 2), null));
        assertThat(result).isEqualTo(GameStatus.DRAW);
        assertThat(game.getEndedBy()).isEqualTo(EndedBy.INSUFFICIENT_MATERIAL);
    }

    @Test
    void kingAndBishopVsKingAndBishopSameSquareColorShouldReturnDraw() {
        board.setPiece(0, 4, null);
        board.setPiece(7, 4, null);
        board.setPiece(0, 0, new King(Color.WHITE));
        board.setPiece(7, 7, new King(Color.BLACK));
        board.setPiece(2, 2, new Bishop(Color.WHITE)); // (2+2)%2 = 0 → light square
        board.setPiece(5, 5, new Bishop(Color.BLACK)); // (5+5)%2 = 0 → light square
        game.setWhiteKingPosition(new Position(0, 0));
        game.setBlackKingPosition(new Position(7, 7));

        GameStatus result = executor.performMove(game, new Move(new Position(0, 0), new Position(1, 0), null));
        assertThat(result).isEqualTo(GameStatus.DRAW);
        assertThat(game.getEndedBy()).isEqualTo(EndedBy.INSUFFICIENT_MATERIAL);
    }

    @Test
    void kingAndBishopVsKingAndBishopDifferentSquareColorShouldNotReturnDraw() {
        board.setPiece(0, 4, null);
        board.setPiece(7, 4, null);
        board.setPiece(0, 0, new King(Color.WHITE));
        board.setPiece(7, 7, new King(Color.BLACK));
        board.setPiece(2, 2, new Bishop(Color.WHITE)); // (2+2)%2 = 0 → light square
        board.setPiece(5, 4, new Bishop(Color.BLACK)); // (5+4)%2 = 1 → dark square
        game.setWhiteKingPosition(new Position(0, 0));
        game.setBlackKingPosition(new Position(7, 7));

        // (1,0) is on B(5,4)'s diagonal so use (0,1) instead
        GameStatus result = executor.performMove(game, new Move(new Position(0, 0), new Position(0, 1), null));
        assertThat(result).isEqualTo(GameStatus.ONGOING);
    }

    @Test
    void kingAndRookVsKingShouldNotReturnDraw() {
        board.setPiece(0, 4, null);
        board.setPiece(7, 4, null);
        board.setPiece(0, 0, new King(Color.WHITE));
        board.setPiece(7, 7, new King(Color.BLACK));
        board.setPiece(3, 3, new Rook(Color.WHITE));
        game.setWhiteKingPosition(new Position(0, 0));
        game.setBlackKingPosition(new Position(7, 7));

        GameStatus result = executor.performMove(game, new Move(new Position(3, 3), new Position(3, 5), null));
        assertThat(result).isEqualTo(GameStatus.ONGOING);
    }

    @Test
    void kingAndKnightVsKingAndKnightShouldNotReturnDraw() {
        // K+N vs K+N: whitePieces=1, blackPieces=1, whiteHasBishop=false
        // → last return short-circuits at whiteHasBishop (line 315 false branch).
        board.setPiece(0, 4, null);
        board.setPiece(7, 4, null);
        board.setPiece(0, 0, new King(Color.WHITE));
        board.setPiece(7, 7, new King(Color.BLACK));
        board.setPiece(3, 3, new Knight(Color.WHITE));
        board.setPiece(5, 5, new Knight(Color.BLACK));
        game.setWhiteKingPosition(new Position(0, 0));
        game.setBlackKingPosition(new Position(7, 7));

        GameStatus result = executor.performMove(game, new Move(new Position(3, 3), new Position(5, 4), null));
        assertThat(result).isEqualTo(GameStatus.ONGOING);
    }

    @Test
    void kingAndBishopVsKingAndKnightShouldNotReturnDraw() {
        // K+B vs K+N: whitePieces=1, blackPieces=1, whiteHasBishop=true, blackHasBishop=false
        // → last return short-circuits at blackHasBishop (line 315 false branch).
        board.setPiece(0, 4, null);
        board.setPiece(7, 4, null);
        board.setPiece(0, 0, new King(Color.WHITE));
        board.setPiece(7, 7, new King(Color.BLACK));
        board.setPiece(4, 4, new Bishop(Color.WHITE));
        board.setPiece(5, 5, new Knight(Color.BLACK));
        game.setWhiteKingPosition(new Position(0, 0));
        game.setBlackKingPosition(new Position(7, 7));

        GameStatus result = executor.performMove(game, new Move(new Position(4, 4), new Position(5, 3), null));
        assertThat(result).isEqualTo(GameStatus.ONGOING);
    }

    // --- castlingRights branches ---
    // Each test places a specific piece at a rook corner to exercise a false branch in castlingRights.
    // A mobile white rook at (4,4) makes the actual move so isGameOver reaches the position-history
    // code path that calls castlingRights. White king stays at (0,4), black king stays at (7,4).

    @Test
    void castlingRightsWhiteQueenSideWrongColorShouldBeIgnored() {
        // Black rook at (0,0): not null, is ROOK, but color != WHITE → line 154 false branch.
        // White blocker at (0,2) prevents the black rook from attacking the white king at (0,4).
        board.setPiece(0, 0, new Rook(Color.BLACK));
        board.setPiece(0, 2, new Rook(Color.WHITE));
        board.setPiece(4, 4, new Rook(Color.WHITE));
        board.setPiece(6, 4, new Pawn(Color.BLACK));

        GameStatus result = executor.performMove(game, new Move(new Position(4, 4), new Position(4, 5), null));
        assertThat(result).isEqualTo(GameStatus.ONGOING);
    }

    @Test
    void castlingRightsWhiteQueenSideMovedRookShouldBeIgnored() {
        // White rook at (0,0) that has already moved: not null, is ROOK, correct color, but hasMoved → line 155 false branch.
        Rook movedRook = new Rook(Color.WHITE);
        movedRook.setMoved();
        board.setPiece(0, 0, movedRook);
        board.setPiece(4, 4, new Rook(Color.WHITE));
        board.setPiece(6, 4, new Pawn(Color.BLACK));

        GameStatus result = executor.performMove(game, new Move(new Position(4, 4), new Position(4, 5), null));
        assertThat(result).isEqualTo(GameStatus.ONGOING);
    }

    @Test
    void castlingRightsWhiteKingSideNonRookShouldBeIgnored() {
        // White queen at (0,7): not null, but pieceType != ROOK → line 162 false branch.
        board.setPiece(0, 7, new Queen(Color.WHITE));
        board.setPiece(4, 4, new Rook(Color.WHITE));
        board.setPiece(6, 4, new Pawn(Color.BLACK));

        GameStatus result = executor.performMove(game, new Move(new Position(4, 4), new Position(4, 5), null));
        assertThat(result).isEqualTo(GameStatus.ONGOING);
    }

    @Test
    void castlingRightsWhiteKingSideWrongColorShouldBeIgnored() {
        // Black rook at (0,7): not null, is ROOK, but color != WHITE → line 163 false branch.
        // White blocker at (0,6) prevents the black rook from attacking the white king at (0,4).
        board.setPiece(0, 7, new Rook(Color.BLACK));
        board.setPiece(0, 6, new Rook(Color.WHITE));
        board.setPiece(4, 4, new Rook(Color.WHITE));
        board.setPiece(6, 4, new Pawn(Color.BLACK));

        GameStatus result = executor.performMove(game, new Move(new Position(4, 4), new Position(4, 5), null));
        assertThat(result).isEqualTo(GameStatus.ONGOING);
    }

    @Test
    void castlingRightsWhiteKingSideMovedRookShouldBeIgnored() {
        // White rook at (0,7) that has already moved → line 164 false branch.
        Rook movedRook = new Rook(Color.WHITE);
        movedRook.setMoved();
        board.setPiece(0, 7, movedRook);
        board.setPiece(4, 4, new Rook(Color.WHITE));
        board.setPiece(6, 4, new Pawn(Color.BLACK));

        GameStatus result = executor.performMove(game, new Move(new Position(4, 4), new Position(4, 5), null));
        assertThat(result).isEqualTo(GameStatus.ONGOING);
    }

    @Test
    void castlingRightsBlackQueenSideWrongColorShouldBeIgnored() {
        // White rook at (7,0): not null, is ROOK, but color != BLACK → line 177 false branch.
        // Black blocker at (7,2) prevents the white rook from attacking the black king at (7,4).
        board.setPiece(7, 0, new Rook(Color.WHITE));
        board.setPiece(7, 2, new Rook(Color.BLACK));
        board.setPiece(4, 4, new Rook(Color.WHITE));
        board.setPiece(1, 4, new Pawn(Color.WHITE));

        GameStatus result = executor.performMove(game, new Move(new Position(4, 4), new Position(4, 5), null));
        assertThat(result).isEqualTo(GameStatus.ONGOING);
    }

    @Test
    void castlingRightsBlackQueenSideMovedRookShouldBeIgnored() {
        // Black rook at (7,0) that has already moved → line 178 false branch.
        Rook movedRook = new Rook(Color.BLACK);
        movedRook.setMoved();
        board.setPiece(7, 0, movedRook);
        board.setPiece(4, 4, new Rook(Color.WHITE));
        board.setPiece(6, 4, new Pawn(Color.BLACK));

        GameStatus result = executor.performMove(game, new Move(new Position(4, 4), new Position(4, 5), null));
        assertThat(result).isEqualTo(GameStatus.ONGOING);
    }

    @Test
    void castlingRightsBlackKingSideWrongColorShouldBeIgnored() {
        // White rook at (7,7): not null, is ROOK, but color != BLACK → line 186 false branch.
        // Black blocker at (7,6) prevents the white rook from attacking the black king at (7,4).
        board.setPiece(7, 7, new Rook(Color.WHITE));
        board.setPiece(7, 6, new Rook(Color.BLACK));
        board.setPiece(4, 4, new Rook(Color.WHITE));
        board.setPiece(1, 4, new Pawn(Color.WHITE));

        GameStatus result = executor.performMove(game, new Move(new Position(4, 4), new Position(4, 5), null));
        assertThat(result).isEqualTo(GameStatus.ONGOING);
    }
}
