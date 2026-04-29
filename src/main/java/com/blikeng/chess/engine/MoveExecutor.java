package com.blikeng.chess.engine;

import com.blikeng.chess.exception.ErrorTypes.InvalidPromotionException;
import com.blikeng.chess.model.Board;
import com.blikeng.chess.entity.GameEntity;
import com.blikeng.chess.model.GameStatus;
import com.blikeng.chess.model.Move;
import com.blikeng.chess.model.Position;
import com.blikeng.chess.model.piece.*;

import java.util.List;

public class MoveExecutor {
    private final MoveGenerator moveGenerator = new MoveGenerator();
    private final SquareAttacked squareAttacked = new SquareAttacked();

    public GameStatus performMove(GameEntity game, Move move, PieceType promotionPiece) {
        Board board = game.getBoard();
        Piece piece = board.getPiece(move.from().row(), move.from().col());

        if (piece == null) return null;

        Color color = piece.getColor();
        if (color == Color.WHITE != game.isWhiteTurn()) return null;

        List<Position> legalMoves = moveGenerator.getPseudoLegalMoves(game, board, move.from());
        if (!legalMoves.contains(move.to())) return null;

        if (kingLeftInCheck(board, game, move, piece, color)) return null;

        piece = checkIfPawnPromotion(piece, move, promotionPiece);

        if (piece.getPieceType() == PieceType.KING){
            handleCastling(board, move, piece);
            updateKingPosition(game, piece, move);
        }

        board.setPiece(move.to().row(), move.to().col(), piece);
        board.setPiece(move.from().row(), move.from().col(), null);
        piece.setMoved();

        return isGameOver(color, board, game);
    }

    private boolean kingLeftInCheck(Board board, GameEntity game, Move move, Piece piece, Color color) {
        Board copy = new Board(board);
        copy.setPiece(move.to().row(), move.to().col(), piece);
        copy.setPiece(move.from().row(), move.from().col(), null);

        Position kingPos = color == Color.WHITE
                ? game.getWhiteKingPosition()
                : game.getBlackKingPosition();

        if (piece.getPieceType() == PieceType.KING) {
            kingPos = move.to();
        }

        Color attacker = color == Color.WHITE ? Color.BLACK : Color.WHITE;

        return squareAttacked.isSquareAttacked(copy, kingPos, attacker);
    }

    private GameStatus isGameOver(Color playerColor, Board board, GameEntity game){
        Color opponentColor = playerColor == Color.WHITE ? Color.BLACK : Color.WHITE;
        boolean hasLegalMove = false;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece p = board.getPiece(row, col);
                if (p == null || p.getColor() != opponentColor) continue;

                Position from = new Position(row, col);
                List<Position> pseudoMoves = moveGenerator.getPseudoLegalMoves(game, board, from);

                for (Position to : pseudoMoves) {
                    if (!kingLeftInCheck(board, game, new Move(from, to), p, opponentColor)) {
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

    private void handleCastling(Board board, Move move, Piece piece) {
        if (piece.getPieceType() != PieceType.KING) return;

        int colDiff = move.to().col() - move.from().col();
        if (Math.abs(colDiff) != 2) return;

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
    }

    private void updateKingPosition(GameEntity game, Piece piece, Move move) {
        if (piece.getPieceType() == PieceType.KING) {
            Position newKingPosition = new Position(move.to().row(), move.to().col());

            if (game.isWhiteTurn()) {
                game.setWhiteKingPosition(newKingPosition);
            } else {
                game.setBlackKingPosition(newKingPosition);
            }
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
