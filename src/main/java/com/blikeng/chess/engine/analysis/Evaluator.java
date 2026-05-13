package com.blikeng.chess.engine.analysis;

import com.blikeng.chess.engine.MoveExecutor;
import com.blikeng.chess.engine.MoveGenerator;
import com.blikeng.chess.model.Board;
import com.blikeng.chess.model.Game;
import com.blikeng.chess.model.Move;
import com.blikeng.chess.model.Position;
import com.blikeng.chess.model.piece.Color;
import com.blikeng.chess.model.piece.Piece;
import com.blikeng.chess.model.piece.PieceType;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Evaluator {
    private Evaluator() {}

    private static final MoveGenerator moveGenerator = new MoveGenerator();
    private static final MoveExecutor moveExecutor = new MoveExecutor();

    public static int miniMax(Game game, int depth, int alpha, int beta) {
        if (depth == 0) return evaluate(game);

        boolean isWhite = game.isWhiteTurn();
        int[] state = {alpha, beta, isWhite ? Integer.MIN_VALUE : Integer.MAX_VALUE};
        Color color = isWhite ? Color.WHITE : Color.BLACK;

        for (int row = 0; row < 8 && state[1] > state[0]; row++)
            for (int col = 0; col < 8 && state[1] > state[0]; col++) {
                Piece piece = game.getBoard().getPiece(row, col);
                if (piece == null || piece.getColor() != color) continue;
                evalMoves(game, new Position(row, col), piece, depth, isWhite, state);
            }

        return state[2];
    }

    private static void evalMoves(Game game, Position from, Piece piece, int depth, boolean isWhite, int[] state) {
        List<Position> moves = moveGenerator.getPseudoLegalMoves(game, game.getBoard(), from);
        for (int m = 0; m < moves.size() && state[1] > state[0]; m++) {
            Position to = moves.get(m);
            PieceType[] promotions = getPromotions(piece, to);
            for (int p = 0; p < promotions.length && state[1] > state[0]; p++) {
                Game copy = new Game(game);
                if (moveExecutor.performMove(copy, new Move(from, to, promotions[p])) != null)
                    updateState(state, miniMax(copy, depth - 1, state[0], state[1]), isWhite);
            }
        }
    }

    private static void updateState(int[] state, int score, boolean isWhite) {
        if (isWhite ? score > state[2] : score < state[2]) state[2] = score;
        if (isWhite) state[0] = Math.max(state[0], score);
        else state[1] = Math.min(state[1], score);
    }

    private static PieceType[] getPromotions(Piece piece, Position to) {
        boolean isPromotion = piece.getPieceType() == PieceType.PAWN
                && ((piece.getColor() == Color.WHITE && to.row() == 7)
                 || (piece.getColor() == Color.BLACK && to.row() == 0));
        return isPromotion
                ? new PieceType[]{PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT}
                : new PieceType[]{null};
    }

    public static MoveEval getBestMove(Game game, int depth) {
        return getBestMove(game, depth, 0);
    }

    public static MoveEval getBestMove(Game game, int depth, int noise) {
        boolean isWhite = game.isWhiteTurn();
        Color color = isWhite ? Color.WHITE : Color.BLACK;
        BestMoveState state = new BestMoveState(isWhite, noise);

        for (int row = 0; row < 8 && state.beta > state.alpha; row++)
            for (int col = 0; col < 8 && state.beta > state.alpha; col++) {
                Piece piece = game.getBoard().getPiece(row, col);
                if (piece == null || piece.getColor() != color) continue;
                evalBestMovesForPiece(game, new Position(row, col), piece, depth, isWhite, state);
            }

        return new MoveEval(state.bestMove, state.best, state.bestPromo);
    }

    private static void evalBestMovesForPiece(Game game, Position from, Piece piece, int depth, boolean isWhite, BestMoveState state) {
        List<Position> moves = moveGenerator.getPseudoLegalMoves(game, game.getBoard(), from);
        for (int m = 0; m < moves.size() && state.beta > state.alpha; m++) {
            Position to = moves.get(m);
            PieceType[] promotions = getPromotions(piece, to);
            for (int p = 0; p < promotions.length && state.beta > state.alpha; p++) {
                Game copy = new Game(game);
                if (moveExecutor.performMove(copy, new Move(from, to, promotions[p])) != null) {
                    int score = miniMax(copy, depth - 1, state.alpha, state.beta);
                    updateBestMoveState(state, score, new Move(from, to, null), promotions[p], isWhite);
                }
            }
        }
    }

    private static void updateBestMoveState(BestMoveState state, int score, Move move, PieceType promotion, boolean isWhite) {
        int noisyScore = state.noise > 0
                ? score + ThreadLocalRandom.current().nextInt(-state.noise, state.noise + 1)
                : score;
        if (isWhite ? noisyScore > state.best : noisyScore < state.best) {
            state.best = noisyScore;
            state.bestMove = move;
            state.bestPromo = promotion;
        }
        if (isWhite) state.alpha = Math.max(state.alpha, score);
        else state.beta = Math.min(state.beta, score);
    }

    static final class BestMoveState {
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        int best;
        Move bestMove = null;
        PieceType bestPromo = null;
        final int noise;

        BestMoveState(boolean isWhite, int noise) {
            best = isWhite ? Integer.MIN_VALUE : Integer.MAX_VALUE;
            this.noise = noise;
        }
    }

    public static int evaluate(Game game) {
        int difference = 0;

        difference += evaluateMaterial(game.getBoard());

        difference += evaluatePawnStructure(game.getBoard()) * -50;

        difference += evaluateMobility(game) * 10;

        return difference;
    }

    private static int evaluateMaterial(Board board) {
        return scoreSide(board, Color.WHITE) - scoreSide(board, Color.BLACK);
    }

    private static int scoreSide(Board board, Color color) {
        int score = 0;
        boolean hasBishop = false;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board.getPiece(row, col);
                if (piece == null || piece.getColor() != color) continue;
                score += piece.getPieceType().getPieceValue();
                if (piece.getPieceType() == PieceType.BISHOP) {
                    if (hasBishop) score += 50;
                    else hasBishop = true;
                }
            }
        }
        return score;
    }

    private static int evaluatePawnStructure(Board board) {
        int[] whiteCols = countPawnsPerColumn(board, Color.WHITE);
        int[] blackCols = countPawnsPerColumn(board, Color.BLACK);
        int blocked = countBlocked(board, Color.WHITE) - countBlocked(board, Color.BLACK);
        int doubled = countDoubled(whiteCols) - countDoubled(blackCols);
        int isolated = countIsolated(whiteCols) - countIsolated(blackCols);
        return blocked + doubled + isolated;
    }

    private static int[] countPawnsPerColumn(Board board, Color color) {
        int[] cols = new int[8];
        for (int row = 0; row < 8; row++)
            for (int col = 0; col < 8; col++) {
                Piece piece = board.getPiece(row, col);
                if (piece != null && piece.getColor() == color && piece.getPieceType() == PieceType.PAWN)
                    cols[col]++;
            }
        return cols;
    }

    private static int countDoubled(int[] pawnsPerCol) {
        int count = 0;
        for (int n : pawnsPerCol)
            if (n > 1) count += n - 1;
        return count;
    }

    private static int countIsolated(int[] pawnsPerCol) {
        int count = 0;
        for (int i = 0; i < 8; i++) {
            if (pawnsPerCol[i] == 0) continue;
            boolean leftEmpty = i == 0 || pawnsPerCol[i - 1] == 0;
            boolean rightEmpty = i == 7 || pawnsPerCol[i + 1] == 0;
            if (leftEmpty && rightEmpty) count++;
        }
        return count;
    }

    private static int countBlocked(Board board, Color color) {
        int count = 0;
        int direction = color == Color.WHITE ? 1 : -1;
        for (int row = 0; row < 8; row++)
            for (int col = 0; col < 8; col++) {
                Piece piece = board.getPiece(row, col);
                if (piece == null || piece.getColor() != color || piece.getPieceType() != PieceType.PAWN) continue;
                int nextRow = row + direction;
                if (nextRow >= 0 && nextRow < 8 && board.getPiece(nextRow, col) != null) count++;
            }
        return count;
    }

    private static int evaluateMobility(Game game) {
        int difference = 0;

        Board board = game.getBoard();

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board.getPiece(row, col);
                if (piece == null) continue;

                int moveCount = moveGenerator.getPseudoLegalMoves(game, board, new Position(row, col)).size();

                if (piece.getColor() == Color.WHITE) {
                    difference += moveCount;
                } else {
                    difference -= moveCount;
                }
            }
        }

        return difference;
    }

    public record MoveEval(Move move, int eval, PieceType promoPiece) {}
}
