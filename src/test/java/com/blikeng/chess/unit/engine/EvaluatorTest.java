package com.blikeng.chess.unit.engine;

import com.blikeng.chess.engine.analysis.Evaluator;
import com.blikeng.chess.model.Board;
import com.blikeng.chess.model.Game;
import com.blikeng.chess.model.Position;
import com.blikeng.chess.model.piece.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluatorTest {

    private Game game;
    private Board board;

    @BeforeEach
    void setup() {
        game = new Game(UUID.randomUUID(), UUID.randomUUID(), "w", UUID.randomUUID(), "b", 800, 800);
        game.setWhiteKingPosition(new Position(0, 4));
        game.setBlackKingPosition(new Position(7, 4));
        board = game.getBoard();
        clearBoard();
        board.setPiece(0, 4, new King(Color.WHITE));
        board.setPiece(7, 4, new King(Color.BLACK));
    }

    private void clearBoard() {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                board.setPiece(r, c, null);
    }


    // --- evaluate ---
    @Test
    void evaluateShouldReturnZeroForSymmetricPosition() {
        assertThat(Evaluator.evaluate(game)).isZero();
    }

    @Test
    void evaluateShouldBePositiveWhenWhiteHasExtraMaterial() {
        board.setPiece(4, 4, new Queen(Color.WHITE));
        assertThat(Evaluator.evaluate(game)).isGreaterThan(0);
    }

    @Test
    void evaluateShouldBeNegativeWhenBlackHasExtraMaterial() {
        board.setPiece(4, 4, new Queen(Color.BLACK));
        assertThat(Evaluator.evaluate(game)).isLessThan(0);
    }

    @Test
    void evaluateShouldGiveBishopPairBonusToWhite() {
        board.setPiece(3, 3, new Bishop(Color.WHITE));
        int scoreWithOne = Evaluator.evaluate(game);
        board.setPiece(3, 5, new Bishop(Color.WHITE));
        int scoreWithTwo = Evaluator.evaluate(game);
        assertThat(scoreWithTwo).isGreaterThan(scoreWithOne);
    }

    @Test
    void evaluateShouldGiveBishopPairBonusToBlack() {
        board.setPiece(3, 3, new Bishop(Color.BLACK));
        int scoreWithOne = Evaluator.evaluate(game);
        board.setPiece(3, 5, new Bishop(Color.BLACK));
        int scoreWithTwo = Evaluator.evaluate(game);
        assertThat(scoreWithTwo).isLessThan(scoreWithOne);
    }

    @Test
    void evaluateShouldPenalizeDoubledPawns() {
        // Adding a doubled pawn should not grant the full +100 material benefit due to the pawn structure penalty
        board.setPiece(3, 3, new Pawn(Color.WHITE));
        int scoreWithSingle = Evaluator.evaluate(game);
        board.setPiece(4, 3, new Pawn(Color.WHITE));
        int scoreWithDoubled = Evaluator.evaluate(game);
        assertThat(scoreWithDoubled).isLessThan(scoreWithSingle + 100);
    }

    @Test
    void evaluateShouldPenalizeIsolatedPawns() {
        // An isolated pawn (no neighbors) incurs a penalty; adding a neighbor removes it
        board.setPiece(3, 4, new Pawn(Color.WHITE));
        int scoreWithIsolated = Evaluator.evaluate(game);
        board.setPiece(3, 5, new Pawn(Color.WHITE));
        int scoreWithNeighbor = Evaluator.evaluate(game);
        // Neighbor removes isolation penalty (-50) on top of adding material (+100), so net gain > 100
        assertThat(scoreWithNeighbor).isGreaterThan(scoreWithIsolated + 100);
    }

    @Test
    void evaluateShouldPenalizeBlockedPawns() {
        // A pawn blocked by any piece incurs a pawn structure penalty
        board.setPiece(3, 3, new Pawn(Color.WHITE));
        int scoreUnblocked = Evaluator.evaluate(game);
        board.setPiece(4, 3, new Rook(Color.BLACK));
        int scoreBlocked = Evaluator.evaluate(game);
        // Blocking adds an enemy piece (negative for white) AND a pawn structure penalty
        assertThat(scoreBlocked).isLessThan(scoreUnblocked);
    }


    // --- miniMax ---
    @Test
    void miniMaxAtDepthZeroShouldReturnEvaluate() {
        board.setPiece(4, 4, new Rook(Color.WHITE));
        int expected = Evaluator.evaluate(game);
        int result = Evaluator.miniMax(game, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void miniMaxAtDepthOneShouldReturnHigherScoreWhenWhiteCanCaptureFreely() {
        // White queen can take a free black rook — score after capture > static eval before it
        board.setPiece(4, 4, new Queen(Color.WHITE));
        board.setPiece(4, 7, new Rook(Color.BLACK));
        int staticEval = Evaluator.evaluate(game);
        int searchResult = Evaluator.miniMax(game, 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertThat(searchResult).isGreaterThan(staticEval);
    }

    @Test
    void miniMaxShouldFavorWhiteWhenWhiteHasFreeCaptureOnBlackPiece() {
        board.setPiece(0, 0, new Rook(Color.WHITE));
        board.setPiece(0, 7, new Rook(Color.BLACK));
        int result = Evaluator.miniMax(game, 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertThat(result).isGreaterThan(0);
    }


    // --- getBestMove ---
    @Test
    void getBestMoveShouldReturnNonNullMove() {
        board.setPiece(1, 0, new Pawn(Color.WHITE));
        board.setPiece(6, 0, new Pawn(Color.BLACK));
        Evaluator.MoveEval result = Evaluator.getBestMove(game, 1);
        assertThat(result.move()).isNotNull();
    }

    @Test
    void getBestMoveShouldReturnPositiveEvalWhenWhiteHasMaterialAdvantage() {
        board.setPiece(4, 4, new Queen(Color.WHITE));
        board.setPiece(4, 5, new Rook(Color.WHITE));
        Evaluator.MoveEval result = Evaluator.getBestMove(game, 1);
        assertThat(result.eval()).isGreaterThan(0);
    }

    @Test
    void getBestMoveShouldSelectCapturingMoveWhenFreeCapture() {
        board.setPiece(4, 4, new Queen(Color.WHITE));
        board.setPiece(4, 7, new Rook(Color.BLACK));
        Evaluator.MoveEval result = Evaluator.getBestMove(game, 1);
        assertThat(result.move()).isNotNull();
        assertThat(result.eval()).isGreaterThan(0);
    }


    // --- evaluatePawnStructure (black side) ---
    @Test
    void evaluateShouldPenalizeBlackDoubledPawns() {
        Pawn p1 = new Pawn(Color.BLACK); p1.setMoved();
        board.setPiece(4, 3, p1);
        int scoreWithSingle = Evaluator.evaluate(game);
        Pawn p2 = new Pawn(Color.BLACK); p2.setMoved();
        board.setPiece(3, 3, p2);
        int scoreWithDoubled = Evaluator.evaluate(game);
        assertThat(scoreWithDoubled).isGreaterThan(scoreWithSingle - 100);
    }

    @Test
    void evaluateShouldPenalizeBlackIsolatedPawns() {
        Pawn p1 = new Pawn(Color.BLACK); p1.setMoved();
        board.setPiece(4, 4, p1);
        int scoreWithIsolated = Evaluator.evaluate(game);
        Pawn p2 = new Pawn(Color.BLACK); p2.setMoved();
        board.setPiece(4, 5, p2);
        int scoreWithNeighbor = Evaluator.evaluate(game);
        assertThat(scoreWithNeighbor).isLessThan(scoreWithIsolated - 100);
    }

    @Test
    void evaluateShouldHandleEdgeColumnPawn() {
        Pawn p = new Pawn(Color.WHITE); p.setMoved();
        board.setPiece(4, 7, p);
        assertThat(Evaluator.evaluate(game)).isGreaterThan(0);
    }

    @Test
    void evaluateShouldHandleBlackEdgeColumnPawn() {
        Pawn p = new Pawn(Color.BLACK); p.setMoved();
        board.setPiece(4, 7, p);
        assertThat(Evaluator.evaluate(game)).isLessThan(0);
    }

    @Test
    void evaluateShouldHandleWhitePawnAtPromotionRank() {
        Pawn p = new Pawn(Color.WHITE); p.setMoved();
        board.setPiece(7, 3, p);
        assertThat(Evaluator.evaluate(game)).isGreaterThan(0);
    }

    @Test
    void evaluateShouldHandleBlackPawnAtBackRank() {
        Pawn p = new Pawn(Color.BLACK); p.setMoved();
        board.setPiece(0, 3, p);
        assertThat(Evaluator.evaluate(game)).isLessThan(0);
    }


    // --- miniMax (pawn paths and black's turn) ---
    @Test
    void miniMaxShouldEvaluateNonPromotingPawnMoves() {
        Pawn p = new Pawn(Color.WHITE); p.setMoved();
        board.setPiece(3, 3, p);
        int result = Evaluator.miniMax(game, 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertThat(result).isGreaterThan(0);
    }

    @Test
    void miniMaxShouldConsiderWhitePawnPromotionInSearch() {
        Pawn p = new Pawn(Color.WHITE); p.setMoved();
        board.setPiece(6, 3, p);
        int result = Evaluator.miniMax(game, 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertThat(result).isGreaterThan(500);
    }

    @Test
    void miniMaxShouldTriggerAlphaBetaCutoff() {
        game.switchTurn();
        board.setPiece(4, 4, new Queen(Color.BLACK));
        // alpha=0: white already has a move scoring 0; black queen makes eval ≈ -900 → beta cuts off immediately
        int result = Evaluator.miniMax(game, 1, 0, Integer.MAX_VALUE);
        assertThat(result).isLessThan(0);
    }

    @Test
    void miniMaxShouldMinimizeForBlackAtDepthOne() {
        game.switchTurn();
        board.setPiece(4, 4, new Queen(Color.BLACK));
        int result = Evaluator.miniMax(game, 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertThat(result).isLessThan(0);
    }

    @Test
    void miniMaxShouldConsiderBlackPawnMovesInSearch() {
        game.switchTurn();
        Pawn p = new Pawn(Color.BLACK); p.setMoved();
        board.setPiece(4, 4, p);
        int result = Evaluator.miniMax(game, 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertThat(result).isLessThan(0);
    }

    @Test
    void miniMaxShouldConsiderBlackPawnPromotionInSearch() {
        game.switchTurn();
        Pawn p = new Pawn(Color.BLACK); p.setMoved();
        board.setPiece(1, 3, p);
        int result = Evaluator.miniMax(game, 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertThat(result).isLessThan(-500);
    }


    // --- miniMax alpha/beta cutoff in promotion loop (line 44) ---

    @Test
    void miniMaxShouldCutOffPromotionLoopWhenAlphaExceedsBeta() {
        // Black pawn about to promote. alpha=-800 means after QUEEN promo (≈-900),
        // beta becomes -900 which is ≤ alpha=-800 → remaining promos (ROOK etc.) pruned.
        game.switchTurn();
        Pawn p = new Pawn(Color.BLACK); p.setMoved();
        board.setPiece(1, 3, p);
        int result = Evaluator.miniMax(game, 1, -800, Integer.MAX_VALUE);
        assertThat(result).isLessThan(-800);
    }

    // --- performMove returning null (line 92 false branch) ---

    @Test
    void getBestMoveShouldSkipIllegalPseudoLegalMoves() {
        // White rook pinned on the a-file: pseudo-legal horizontal moves leave king in check → performMove returns null
        King king = new King(Color.WHITE); king.setMoved();
        board.setPiece(0, 0, king);
        board.setPiece(3, 0, new Rook(Color.WHITE));
        board.setPiece(7, 0, new Rook(Color.BLACK));
        game.setWhiteKingPosition(new Position(0, 0));
        Evaluator.MoveEval result = Evaluator.getBestMove(game, 1);
        assertThat(result.move()).isNotNull();
    }

    // --- getBestMove with noise ---

    @Test
    void getBestMoveNoiseOverloadWithZeroNoiseShouldReturnSameResultAsSingleArgVersion() {
        board.setPiece(4, 4, new Queen(Color.WHITE));
        board.setPiece(4, 7, new Rook(Color.BLACK));
        Evaluator.MoveEval withoutNoise = Evaluator.getBestMove(game, 1);
        Evaluator.MoveEval withZeroNoise = Evaluator.getBestMove(game, 1, 0);
        assertThat(withZeroNoise.move()).isEqualTo(withoutNoise.move());
        assertThat(withZeroNoise.eval()).isEqualTo(withoutNoise.eval());
    }

    @Test
    void getBestMoveWithNoiseShouldReturnLegalMove() {
        board.setPiece(1, 0, new Pawn(Color.WHITE));
        board.setPiece(6, 0, new Pawn(Color.BLACK));
        Evaluator.MoveEval result = Evaluator.getBestMove(game, 1, 500);
        assertThat(result.move()).isNotNull();
    }

    @Test
    void getBestMoveAlphaBetaUsesActualScoreNotNoisyScore() {
        board.setPiece(4, 0, new Queen(Color.WHITE));
        board.setPiece(4, 7, new Rook(Color.BLACK));
        Evaluator.MoveEval result = Evaluator.getBestMove(game, 1, 1000);

        assertThat(result.move()).isNotNull();
        assertThat(result.eval()).isNotEqualTo(Integer.MIN_VALUE);
    }

    @Test
    void getBestMoveAlphaBetaCutoffInOuterLoopShouldStillFindBestMove() {
        board.setPiece(4, 0, new Queen(Color.WHITE));
        board.setPiece(3, 1, new Rook(Color.WHITE));
        board.setPiece(4, 7, new Rook(Color.BLACK));
        Evaluator.MoveEval result = Evaluator.getBestMove(game, 1);
        assertThat(result.move()).isNotNull();
        assertThat(result.eval()).isGreaterThan(0);
    }

    // --- getBestMove (black's turn) ---
    @Test
    void getBestMoveShouldReturnMoveForBlack() {
        game.switchTurn();
        board.setPiece(4, 4, new Queen(Color.BLACK));
        Pawn p = new Pawn(Color.BLACK); p.setMoved();
        board.setPiece(4, 3, p);
        Evaluator.MoveEval result = Evaluator.getBestMove(game, 1);
        assertThat(result.move()).isNotNull();
        assertThat(result.eval()).isLessThan(0);
    }

    @Test
    void getBestMoveShouldPreferQueenPromotionForWhitePawn() {
        Pawn pawn = new Pawn(Color.WHITE);
        pawn.setMoved();
        board.setPiece(6, 3, pawn);
        Evaluator.MoveEval result = Evaluator.getBestMove(game, 1);
        assertThat(result.move()).isNotNull();
        assertThat(result.promoPiece()).isEqualTo(PieceType.QUEEN);
    }

    @Test
    void getBestMoveAtDepthTwoShouldFindBestMove() {
        board.setPiece(1, 0, new Rook(Color.WHITE));
        board.setPiece(2, 0, new Rook(Color.WHITE));
        board.setPiece(6, 7, new Rook(Color.BLACK));
        Evaluator.MoveEval result = Evaluator.getBestMove(game, 2);
        assertThat(result.move()).isNotNull();
        assertThat(result.eval()).isGreaterThan(0);
    }

    @Test
    void getBestMoveShouldPreferQueenPromotionForBlackPawn() {
        game.switchTurn();
        Pawn pawn = new Pawn(Color.BLACK);
        pawn.setMoved();
        board.setPiece(1, 3, pawn);
        Evaluator.MoveEval result = Evaluator.getBestMove(game, 1);
        assertThat(result.move()).isNotNull();
        assertThat(result.promoPiece()).isEqualTo(PieceType.QUEEN);
    }

}
