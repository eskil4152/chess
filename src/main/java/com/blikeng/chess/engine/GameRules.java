package com.blikeng.chess.engine;

import com.blikeng.chess.exception.types.InvalidPromotionException;
import com.blikeng.chess.model.*;
import com.blikeng.chess.model.piece.*;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Move-legality and game-state rules used by {@link MoveExecutor}.
 *
 * <p>Covers three things: whether a move would leave the mover's own king in check
 * ({@link #kingLeftInCheck}), pawn-promotion resolution ({@link #checkIfPawnPromotion}),
 * and end-of-game detection ({@link #isGameOver}).
 */
public class GameRules {
    private final MoveGenerator moveGenerator = new MoveGenerator();
    private final SquareAttacked squareAttacked = new SquareAttacked(moveGenerator);

    /**
     * Determines the outcome after {@code playerColor} has just moved, judged from the
     * opponent's side (the player now to move). Conditions are checked in order:
     * <ol>
     *   <li>Fifty-move rule (halfmove clock ≥ 100) → draw.</li>
     *   <li>Opponent has no legal move → checkmate (a win for {@code playerColor}) if their
     *       king is in check, otherwise stalemate (draw).</li>
     *   <li>Insufficient material → draw.</li>
     *   <li>Threefold repetition → draw.</li>
     * </ol>
     * If none apply, the turn is switched to the opponent and {@link GameStatus#ONGOING}
     * is returned.
     *
     * <p>Side effects: sets the game's {@link EndedBy} reason on a terminal result, records
     * the current position for repetition tracking, and switches the turn only when play
     * continues (never on a game-ending result).
     */
    public GameStatus isGameOver(Color playerColor, Board board, Game game){
        if (game.getHalfMoveClock() >= 100) {
            game.setEndedBy(EndedBy.FIFTY_MOVE_RULE);
            return GameStatus.DRAW;
        }

        Color opponentColor = playerColor == Color.WHITE ? Color.BLACK : Color.WHITE;
        boolean hasLegalMove = false;

        for (int row = 0; row < 8 && !hasLegalMove; row++)
            for (int col = 0; col < 8 && !hasLegalMove; col++) {
                Piece piece = board.getPiece(row, col);
                if (piece != null && piece.getColor() == opponentColor)
                    hasLegalMove = hasAnyLegalMove(board, game, piece, new Position(row, col), opponentColor);
            }

        if (!hasLegalMove) return noLegalMoveStatus(game, playerColor, opponentColor);

        if (isInsufficientMaterial(board)) {
            game.setEndedBy(EndedBy.INSUFFICIENT_MATERIAL);
            return GameStatus.DRAW;
        }

        if (isThreefoldRepetition(game, board)) {
            game.setEndedBy(EndedBy.REPETITION);
            return GameStatus.DRAW;
        }

        game.switchTurn();
        return GameStatus.ONGOING;
    }

    public boolean kingLeftInCheck(Board board, Game game, Move move, Piece piece, Color color, boolean isEnPassant) {
        Board copy = new Board(board);
        piece = checkIfPawnPromotion(piece, move);
        copy.setPiece(move.to().row(), move.to().col(), piece);
        copy.setPiece(move.from().row(), move.from().col(), null);
        if (isEnPassant) {
            copy.setPiece(move.from().row(), move.to().col(), null);
        }

        Position kingPos = color == Color.WHITE
            ? game.getWhiteKingPosition()
            : game.getBlackKingPosition();

        if (piece.getPieceType() == PieceType.KING) {
            kingPos = move.to();
        }

        Color attacker = color == Color.WHITE ? Color.BLACK : Color.WHITE;

        return squareAttacked.isSquareAttacked(copy, game, kingPos, attacker);
    }

    public Piece checkIfPawnPromotion(Piece piece, Move move) {
        if (piece.getPieceType() != PieceType.PAWN) return piece;
        PieceType promotionPiece = move.promotionPiece();

        if (piece.getColor() == Color.WHITE && move.to().row() == 7) {
            if (promotionPiece == null) throw new InvalidPromotionException();
            return createPromotionPiece(promotionPiece, Color.WHITE);
        }

        if (piece.getColor() == Color.BLACK && move.to().row() == 0) {
            if (promotionPiece == null) throw new InvalidPromotionException();
            return createPromotionPiece(promotionPiece, Color.BLACK);
        }

        return piece;
    }

    private Piece createPromotionPiece(PieceType promotionPiece, Color color) {
        return switch (promotionPiece){
            case QUEEN -> new Queen(color);
            case ROOK -> new Rook(color);
            case BISHOP -> new Bishop(color);
            case KNIGHT -> new Knight(color);
            default -> throw new InvalidPromotionException();
        };
    }

    private boolean hasAnyLegalMove(Board board, Game game, Piece piece, Position from, Color color) {
        for (Position to : moveGenerator.getPseudoLegalMoves(game, board, from)) {
            boolean isEnPassantMove = piece.getPieceType() == PieceType.PAWN && to.equals(game.getEnPassantTarget());
            boolean isPromotion = piece.getPieceType() == PieceType.PAWN
                && ((color == Color.WHITE && to.row() == 7) || (color == Color.BLACK && to.row() == 0));
            PieceType promo = isPromotion ? PieceType.QUEEN : null;
            if (!kingLeftInCheck(board, game, new Move(from, to, promo), piece, color, isEnPassantMove)) {
                return true;
            }
        }
        return false;
    }

    private GameStatus noLegalMoveStatus(Game game, Color playerColor, Color opponentColor) {
        if (squareAttacked.isInCheck(game, opponentColor)) {
            game.setEndedBy(EndedBy.CHECKMATE);
            return playerColor == Color.WHITE ? GameStatus.WHITE_WIN : GameStatus.BLACK_WIN;
        }
        game.setEndedBy(EndedBy.STALEMATE);
        return GameStatus.DRAW;
    }

    private boolean isThreefoldRepetition(Game game, Board board) {
        String key = board.toString() + game.isWhiteTurn() + game.getEnPassantTarget() + Arrays.toString(castlingRights(game));
        HashMap<String, Integer> history = game.getPositionHistory();
        history.put(key, history.getOrDefault(key, 0) + 1);
        return history.get(key) >= 3;
    }

    private boolean isInsufficientMaterial(Board board) {
        MaterialInfo white = scanMaterial(board, Color.WHITE);
        MaterialInfo black = scanMaterial(board, Color.BLACK);

        if (white.pieceCount() == 0 && black.pieceCount() == 0) return true;
        if (white.pieceCount() == 1 && black.pieceCount() == 0 && (white.hasBishop() || white.hasKnight())) return true;
        if (black.pieceCount() == 1 && white.pieceCount() == 0 && (black.hasBishop() || black.hasKnight())) return true;

        return white.pieceCount() == 1 && black.pieceCount() == 1
            && white.hasBishop() && black.hasBishop()
            && white.bishopSquareColor() == black.bishopSquareColor();
    }

    /**
     * Returns the four castling-availability flags, ordered
     * {@code [white kingside, white queenside, black kingside, black queenside]}.
     *
     * <p>Used only to build the threefold-repetition key (so positions that look identical
     * but differ in castling rights aren't counted as repetitions). Actual castling
     * legality is handled by {@link MoveExecutor}, not here.
     */
    private boolean[] castlingRights(Game game){
        boolean whiteCanCastleKingSide = false;
        boolean whiteCanCastleQueenSide = false;
        boolean blackCanCastleKingSide = false;
        boolean blackCanCastleQueenSide = false;

        Board board = game.getBoard();

        if (!board.getPiece(game.getWhiteKingPosition().row(), game.getWhiteKingPosition().col()).hasMoved()) {
            Piece kingSideRook = board.getPiece(game.getWhiteKingPosition().row(), 7);
            Piece queenSideRook = board.getPiece(game.getWhiteKingPosition().row(), 0);

            if (
                queenSideRook != null &&
                    queenSideRook.getPieceType() == PieceType.ROOK &&
                    queenSideRook.getColor() == Color.WHITE &&
                    !queenSideRook.hasMoved()
            ) {
                whiteCanCastleQueenSide = true;
            }

            if (
                kingSideRook != null &&
                    kingSideRook.getPieceType() == PieceType.ROOK &&
                    kingSideRook.getColor() == Color.WHITE &&
                    !kingSideRook.hasMoved()
            ){
                whiteCanCastleKingSide = true;
            }
        }

        if (!board.getPiece(game.getBlackKingPosition().row(), game.getBlackKingPosition().col()).hasMoved()) {
            Piece kingSideRook = board.getPiece(game.getBlackKingPosition().row(), 7);
            Piece queenSideRook = board.getPiece(game.getBlackKingPosition().row(), 0);

            if (
                queenSideRook != null &&
                    queenSideRook.getPieceType() == PieceType.ROOK &&
                    queenSideRook.getColor() == Color.BLACK &&
                    !queenSideRook.hasMoved()
            ) {
                blackCanCastleQueenSide = true;
            }

            if (
                kingSideRook != null &&
                    kingSideRook.getPieceType() == PieceType.ROOK &&
                    kingSideRook.getColor() == Color.BLACK &&
                    !kingSideRook.hasMoved()
            ) {
                blackCanCastleKingSide = true;
            }
        }

        return new boolean[]{whiteCanCastleKingSide, whiteCanCastleQueenSide, blackCanCastleKingSide, blackCanCastleQueenSide};
    }

    private MaterialInfo scanMaterial(Board board, Color color) {
        int pieces = 0;
        boolean hasBishop = false;
        boolean hasKnight = false;
        int bishopSquareColor = -1;
        for (int row = 0; row < 8; row++)
            for (int col = 0; col < 8; col++) {
                Piece piece = board.getPiece(row, col);
                if (piece == null || piece.getPieceType() == PieceType.KING || piece.getColor() != color) continue;
                pieces++;
                if (piece.getPieceType() == PieceType.BISHOP) { hasBishop = true; bishopSquareColor = (row + col) % 2; }
                else if (piece.getPieceType() == PieceType.KNIGHT) hasKnight = true;
            }
        return new MaterialInfo(pieces, hasBishop, hasKnight, bishopSquareColor);
    }

    private record MaterialInfo(int pieceCount, boolean hasBishop, boolean hasKnight, int bishopSquareColor) {}
}
