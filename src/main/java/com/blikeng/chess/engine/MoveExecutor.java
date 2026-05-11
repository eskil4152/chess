package com.blikeng.chess.engine;

import com.blikeng.chess.exception.errorTypes.InvalidPromotionException;
import com.blikeng.chess.model.*;
import com.blikeng.chess.model.piece.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MoveExecutor {
    private final MoveGenerator moveGenerator = new MoveGenerator();
    private final SquareAttacked squareAttacked = new SquareAttacked(moveGenerator);

    public GameStatus performMove(Game game, Move move) {
        Board board = game.getBoard();
        Piece piece = board.getPiece(move.from().row(), move.from().col());

        if (piece == null) return null;

        Color color = piece.getColor();
        if (color == Color.WHITE != game.isWhiteTurn()) return null;

        List<Position> legalMoves = moveGenerator.getPseudoLegalMoves(game, board, move.from());
        if (!legalMoves.contains(move.to())) return null;

        boolean isEnPassant = piece.getPieceType() == PieceType.PAWN
                && move.to().equals(game.getEnPassantTarget());

        if (kingLeftInCheck(board, game, move, piece, color, isEnPassant)) return null;

        boolean isCapture = board.getPiece(move.to().row(), move.to().col()) != null || isEnPassant;
        if (piece.getPieceType() == PieceType.PAWN || isCapture){
            game.setHalfMoveClock(0);
        } else {
            game.setHalfMoveClock(game.getHalfMoveClock() + 1);
        }

        piece = checkIfPawnPromotion(piece, move);

        if (piece.getPieceType() == PieceType.KING){
            handleCastling(board, game, move, piece);
            updateKingPosition(game, move);
        }

        if (isEnPassant) {
            board.setPiece(move.from().row(), move.to().col(), null);
        }

        updateEnPassantTarget(game, piece, move);

        board.setPiece(move.to().row(), move.to().col(), piece);
        board.setPiece(move.from().row(), move.from().col(), null);
        piece.setMoved();

        return isGameOver(color, board, game);
    }

    private boolean kingLeftInCheck(Board board, Game game, Move move, Piece piece, Color color, boolean isEnPassant) {
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

    private GameStatus isGameOver(Color playerColor, Board board, Game game){
        if (game.getHalfMoveClock() >= 100) {
            game.setEndedBy(EndedBy.FIFTY_MOVE_RULE);
            return GameStatus.DRAW;
        }

        Color opponentColor = playerColor == Color.WHITE ? Color.BLACK : Color.WHITE;
        boolean hasLegalMove = false;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece p = board.getPiece(row, col);
                if (p == null || p.getColor() != opponentColor) continue;

                Position from = new Position(row, col);
                List<Position> pseudoMoves = moveGenerator.getPseudoLegalMoves(game, board, from);

                for (Position to : pseudoMoves) {
                    boolean epMove = p.getPieceType() == PieceType.PAWN && to.equals(game.getEnPassantTarget());
                    if (!kingLeftInCheck(board, game, new Move(from, to, null), p, opponentColor, epMove)) {
                        hasLegalMove = true;
                        break;
                    }
                }

                if (hasLegalMove) break;
            }
        }

        if (!hasLegalMove) {
            if (squareAttacked.isInCheck(game, opponentColor)) {
                game.setEndedBy(EndedBy.CHECKMATE);
                return playerColor == Color.WHITE ? GameStatus.WHITE_WIN : GameStatus.BLACK_WIN;
            } else {
                game.setEndedBy(EndedBy.STALEMATE);
                return GameStatus.DRAW;
            }
        }

        game.switchTurn();

        String key = board.toString() + game.isWhiteTurn() + game.getEnPassantTarget() + Arrays.toString(castlingRights(game));
        HashMap<String, Integer> positionHistory = game.getPositionHistory();
        positionHistory.put(key, positionHistory.getOrDefault(key, 0) + 1);

        if (positionHistory.get(key) >= 3) {
            game.setEndedBy(EndedBy.REPETITION);
            return GameStatus.DRAW;
        }

        return GameStatus.ONGOING;
    }

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

    private void handleCastling(Board board, Game game, Move move, Piece piece) {
        int colDiff = move.to().col() - move.from().col();
        if (Math.abs(colDiff) != 2) return;

        if (!canCastle(board, game, colDiff, piece.getColor())) return;

        int row = move.from().row();

        Piece rook;
        if (colDiff == 2) {
            rook = board.getPiece(row, 7);
            board.setPiece(row, 5, rook);
            board.setPiece(row, 7, null);
        } else {
            rook = board.getPiece(row, 0);
            board.setPiece(row, 3, rook);
            board.setPiece(row, 0, null);
        }

        rook.setMoved();
    }

    private boolean canCastle(Board board, Game game, int colDiff, Color color) {
        Color attacker = color == Color.WHITE ? Color.BLACK : Color.WHITE;
        int row = color == Color.WHITE ? 0 : 7;
        int transitCol = colDiff == 2 ? 5 : 3;

        return !squareAttacked.isSquareAttacked(board, game, new Position(row, 4), attacker)
            && !squareAttacked.isSquareAttacked(board, game, new Position(row, transitCol), attacker);
    }

    private void updateKingPosition(Game game, Move move) {
        Position newKingPosition = new Position(move.to().row(), move.to().col());

        if (game.isWhiteTurn()) {
            game.setWhiteKingPosition(newKingPosition);
        } else {
            game.setBlackKingPosition(newKingPosition);
        }
    }

    private void updateEnPassantTarget(Game game, Piece piece, Move move) {
        if (piece.getPieceType() == PieceType.PAWN && Math.abs(move.to().row() - move.from().row()) == 2) {
            int epRow = (move.from().row() + move.to().row()) / 2;
            game.setEnPassantTarget(new Position(epRow, move.from().col()));
        } else {
            game.setEnPassantTarget(null);
        }
    }

    private Piece checkIfPawnPromotion(Piece piece, Move move) {
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
}
