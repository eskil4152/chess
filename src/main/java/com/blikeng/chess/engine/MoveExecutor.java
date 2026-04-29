package com.blikeng.chess.engine;

import com.blikeng.chess.exception.ErrorTypes.InvalidPromotionException;
import com.blikeng.chess.model.*;
import com.blikeng.chess.model.piece.*;

import java.util.List;

public class MoveExecutor {
    private final MoveGenerator moveGenerator = new MoveGenerator();
    private final SquareAttacked squareAttacked = new SquareAttacked(moveGenerator);

    public GameStatus performMove(Game game, Move move, PieceType promotionPiece) {
        boolean isCastling = false;

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

        piece = checkIfPawnPromotion(piece, move, promotionPiece);

        if (piece.getPieceType() == PieceType.KING){
            isCastling = handleCastling(board, move, piece);
            updateKingPosition(game, piece, move);
        }

        if (isEnPassant) {
            board.setPiece(move.from().row(), move.to().col(), null);
        }

        updateEnPassantTarget(game, piece, move);

        board.setPiece(move.to().row(), move.to().col(), piece);
        board.setPiece(move.from().row(), move.from().col(), null);
        piece.setMoved();

        game.addMove(new MoveRecord(
                move,
                piece.getPieceType(),
                isEnPassant,
                isCastling
        ));

        return isGameOver(color, board, game);
    }

    private boolean kingLeftInCheck(Board board, Game game, Move move, Piece piece, Color color, boolean isEnPassant) {
        Board copy = new Board(board);
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
                    if (!kingLeftInCheck(board, game, new Move(from, to), p, opponentColor, epMove)) {
                        hasLegalMove = true;
                        break;
                    }
                }

                if (hasLegalMove) break;
            }
        }

        if (!hasLegalMove) {
            if (squareAttacked.isInCheck(game, opponentColor)) {
                return playerColor == Color.WHITE ? GameStatus.WHITE_WIN : GameStatus.BLACK_WIN;
            } else {
                return GameStatus.DRAW;
            }
        }

        game.switchTurn();
        return GameStatus.ONGOING;
    }

    private boolean handleCastling(Board board, Move move, Piece piece) {
        if (piece.getPieceType() != PieceType.KING) return false;

        int colDiff = move.to().col() - move.from().col();
        if (Math.abs(colDiff) != 2) return false;

        int row = move.from().row();

        if (colDiff == 2) {
            Piece rook = board.getPiece(row, 7);
            board.setPiece(row, 5, rook);
            board.setPiece(row, 7, null);
            rook.setMoved();
        } else {
            Piece rook = board.getPiece(row, 0);
            board.setPiece(row, 3, rook);
            board.setPiece(row, 0, null);
            rook.setMoved();
        }

        return true;
    }

    private void updateKingPosition(Game game, Piece piece, Move move) {
        if (piece.getPieceType() == PieceType.KING) {
            Position newKingPosition = new Position(move.to().row(), move.to().col());

            if (game.isWhiteTurn()) {
                game.setWhiteKingPosition(newKingPosition);
            } else {
                game.setBlackKingPosition(newKingPosition);
            }
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

    private Piece checkIfPawnPromotion(Piece piece, Move move, PieceType promotionPiece) {
        if (piece.getPieceType() == PieceType.PAWN) {
            switch (piece.getColor()){
                case WHITE -> {
                    if (move.to().row() == 0 && promotionPiece != null){
                        piece = createPromotionPiece(promotionPiece, Color.WHITE);
                    }
                }

                case BLACK -> {
                    if (move.to().row() == 7 && promotionPiece != null){
                        piece = createPromotionPiece(promotionPiece, Color.BLACK);
                    }
                }
            }
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
