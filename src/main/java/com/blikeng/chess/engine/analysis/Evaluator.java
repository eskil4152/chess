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

public class Evaluator {
    private static final MoveGenerator moveGenerator = new MoveGenerator();
    private static final MoveExecutor moveExecutor = new MoveExecutor();

    public static int miniMax(Game game, int depth, int alpha, int beta) {
        if (depth == 0) return evaluate(game);

        int best = game.isWhiteTurn() ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        outer:
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = game.getBoard().getPiece(row, col);
                Color color = game.isWhiteTurn() ? Color.WHITE : Color.BLACK;

                if (piece == null || piece.getColor() != color) continue;

                List<Position> legalMoves = moveGenerator.getPseudoLegalMoves(game, game.getBoard(), new Position(row, col));
                for (Position legalMove : legalMoves) {
                    Move move = new Move(new Position(row, col), legalMove);
                    boolean isPromotion = piece.getPieceType() == PieceType.PAWN
                            && (game.isWhiteTurn() ? legalMove.row() == 7 : legalMove.row() == 0);

                    PieceType[] promotions = isPromotion
                            ? new PieceType[]{PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT}
                            : new PieceType[]{null};

                    for (PieceType promo : promotions) {
                        Game copy = new Game(game);
                        if (moveExecutor.performMove(copy, move, promo) == null) continue;
                        int current = miniMax(copy, depth - 1, alpha, beta);

                        if (game.isWhiteTurn()) {
                            best = Math.max(best, current);
                            alpha = Math.max(alpha, current);
                        } else {
                            best = Math.min(best, current);
                            beta = Math.min(beta, current);
                        }

                        if (beta <= alpha) break outer;
                    }
                }
            }
        }

        return best;
    }

    public static MoveEval getBestMove(Game game, int depth) {
        Move bestMove = null;
        PieceType bestPromo = null;

        boolean isWhite = game.isWhiteTurn();
        int best = game.isWhiteTurn() ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        outer:
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = game.getBoard().getPiece(row, col);
                Color color = game.isWhiteTurn() ? Color.WHITE : Color.BLACK;
                if (piece == null || piece.getColor() != color) continue;

                List<Position> legalMoves = moveGenerator.getPseudoLegalMoves(game, game.getBoard(), new Position(row, col));
                for (Position legalMove : legalMoves) {
                    Move move = new Move(new Position(row, col), legalMove);
                    boolean isPromotion = piece.getPieceType() == PieceType.PAWN
                            && (isWhite ? legalMove.row() == 7 : legalMove.row() == 0);

                    PieceType[] promotions = isPromotion
                            ? new PieceType[]{PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT}
                            : new PieceType[]{null};

                    for (PieceType promo : promotions) {
                        Game copy = new Game(game);
                        if (moveExecutor.performMove(copy, move, promo) == null) continue;
                        int score = miniMax(copy, depth - 1, alpha, beta);

                        if (isWhite ? score > best : score < best) {
                            best = score;
                            bestMove = move;
                            bestPromo = promo;
                        }

                        if (isWhite) {
                            alpha = Math.max(alpha, score);
                        } else {
                            beta = Math.min(beta, score);
                        }

                        if (beta <= alpha) break outer;
                    }
                }
            }
        }

        return new MoveEval(bestMove, best, bestPromo);
    }

    public static int evaluate(Game game) {
        int difference = 0;

        difference += evaluateMaterial(game.getBoard());

        difference += evaluatePawnStructure(game.getBoard()) * -50;

        difference += evaluateMobility(game) * 10;

        return difference;
    }

    private static int evaluateMaterial(Board board) {
        int difference = 0;

        boolean whiteBishop = false;
        boolean blackBishop = false;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board.getPiece(row, col);
                if (piece == null) continue;

                if (piece.getColor() == Color.WHITE) {
                    difference += piece.getPieceType().getPieceValue();

                    if (piece.getPieceType() == PieceType.BISHOP) {
                        if (whiteBishop) difference += 50;
                        else whiteBishop = true;
                    }
                } else {
                    difference -= piece.getPieceType().getPieceValue();

                    if (piece.getPieceType() == PieceType.BISHOP) {
                        if (blackBishop) difference -= 50;
                        else blackBishop = true;
                    }
                }
            }
        }

        return difference;
    }

    private static int evaluatePawnStructure(Board board) {
        int difference = 0;

        int whiteBlockedPawns = 0;
        int blackBlockedPawns = 0;

        int whiteIsolatedPawns = 0;
        int blackIsolatedPawns = 0;

        int whiteDoubledPawns = 0;
        int blackDoubledPawns = 0;

        int[] whitePawnsColumn = new int[8];
        int[] blackPawnsColumn = new int[8];

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board.getPiece(row, col);
                if (piece == null || piece.getPieceType() != PieceType.PAWN) continue;

                if (piece.getColor() == Color.WHITE) {
                    whitePawnsColumn[col]++;

                    if (row == 7) continue;
                    if (board.getPiece(row + 1, col) != null) whiteBlockedPawns++;
                } else {
                    blackPawnsColumn[col]++;

                    if (row == 0) continue;
                    if (board.getPiece(row - 1, col) != null) blackBlockedPawns++;
                }
            }
        }

        for (int i = 0; i < 8; i++) {
            if (whitePawnsColumn[i] > 1) whiteDoubledPawns += whitePawnsColumn[i] - 1;
            if (whitePawnsColumn[i] > 0){
                boolean hasLeftColumn = i > 0;
                boolean hasRightColumn = i < 7;

                boolean leftEmpty = !hasLeftColumn || whitePawnsColumn[i - 1] == 0;
                boolean rightEmpty = !hasRightColumn || whitePawnsColumn[i + 1] == 0;
                if (leftEmpty && rightEmpty) whiteIsolatedPawns++;
            }

            if (blackPawnsColumn[i] > 1) blackDoubledPawns += blackPawnsColumn[i] - 1;
            if (blackPawnsColumn[i] > 0){
                boolean hasLeftColumn = i > 0;
                boolean hasRightColumn = i < 7;

                boolean leftEmpty = !hasLeftColumn || blackPawnsColumn[i - 1] == 0;
                boolean rightEmpty = !hasRightColumn || blackPawnsColumn[i + 1] == 0;
                if (leftEmpty && rightEmpty) blackIsolatedPawns++;
            }
        }

        difference += whiteBlockedPawns - blackBlockedPawns;
        difference += whiteDoubledPawns - blackDoubledPawns;
        difference += whiteIsolatedPawns - blackIsolatedPawns;

        return difference;
    }

    private static int evaluateMobility(Game game) {
        int difference = 0;

        Board board = game.getBoard();

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = board.getPiece(row, col);
                if (piece == null) continue;

                int legalMoves = moveGenerator.getPseudoLegalMoves(game, board, new Position(row, col)).size();

                if (piece.getColor() == Color.WHITE) {
                    difference += legalMoves;
                } else {
                    difference -= legalMoves;
                }
            }
        }

        return difference;
    }

    public record MoveEval(Move move, int eval, PieceType promoPiece) {}
}
