package com.blikeng.chess.engine;

import com.blikeng.chess.model.Board;
import com.blikeng.chess.model.Game;
import com.blikeng.chess.model.Position;
import com.blikeng.chess.model.piece.Color;
import com.blikeng.chess.model.piece.Piece;
import com.blikeng.chess.model.piece.PieceType;

import java.util.ArrayList;
import java.util.List;

public class MoveGenerator {
    public List<Position> getPseudoLegalMoves(Game game, Board board, Position position) {
        Piece piece = board.getPiece(position.row(), position.col());

        if (piece == null) return List.of();

        return switch (piece.getPieceType()) {
            case PieceType.ROOK -> getRookMoves(board, position);
            case PieceType.KNIGHT -> getKnightMoves(board, position);
            case PieceType.BISHOP -> getBishopMoves(board, position);
            case PieceType.QUEEN -> getQueenMoves(board, position);
            case PieceType.KING -> getKingMoves(board, position);
            case PieceType.PAWN -> getPawnMoves(game, board, position);
        };
    }

    private List<Position> getRookMoves(Board board, Position position) {
        int[][] directions = {
            {-1, 0}, {+1, 0},
            {0, -1}, {0, +1}
        };

        return slideDirection(board, position, directions);
    }

    private List<Position> getKnightMoves(Board board, Position position) {
        List<Position> moves = new ArrayList<>();

        int[][] offsets = {
            {-2, -1}, {-2, +1},
            {-1, -2}, {-1, +2},
            {+1, -2}, {+1, +2},
            {+2, -1}, {+2, +1}
        };

        for (int[] offset : offsets) {
            int newRow = position.row() + offset[0];
            int newCol = position.col() + offset[1];

            if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8 &&
                    (board.getPiece(newRow, newCol) == null ||
                    board.getPiece(newRow, newCol).getColor() != board.getPiece(position.row(), position.col()).getColor())) {
                moves.add(new Position(newRow, newCol));
            }
        }

        return moves;
    }

    private List<Position> getBishopMoves(Board board, Position position) {
        int[][] directions = {
                {-1, -1}, {-1, +1},
                {+1, -1}, {+1, +1}
        };

        return slideDirection(board, position, directions);
    }

    private List<Position> getQueenMoves(Board board, Position position) {
        int[][] directions = {
            {-1, -1}, {-1, +1},
            {+1, -1}, {+1, +1},
            {-1, 0}, {+1, 0},
            {0, -1}, {0, +1}
        };

        return slideDirection(board, position, directions);
    }

    private List<Position> getKingMoves(Board board, Position position) {
        List<Position> moves = new ArrayList<>();
        addAdjacentKingMoves(board, position, moves);

        Piece king = board.getPiece(position.row(), position.col());
        if (!king.hasMoved()) {
            int row = king.getColor() == Color.WHITE ? 0 : 7;
            addCastlingMoves(board, row, moves);
        }

        return moves;
    }

    private void addAdjacentKingMoves(Board board, Position position, List<Position> moves) {
        int[][] directions = {
            {-1, -1}, {-1, 0}, {-1, +1},
            {0, -1}, {0, +1},
            {+1, -1}, {+1, 0}, {+1, +1}
        };

        Piece moving = board.getPiece(position.row(), position.col());
        for (int[] direction : directions) {
            int row = position.row() + direction[0];
            int col = position.col() + direction[1];

            if (row >= 0 && row < 8 && col >= 0 && col < 8) {
                Piece target = board.getPiece(row, col);
                if (target == null || target.getColor() != moving.getColor()) {
                    moves.add(new Position(row, col));
                }
            }
        }
    }

    private void addCastlingMoves(Board board, int row, List<Position> moves) {
        Piece kingsideRook = board.getPiece(row, 7);
        if (kingsideRook != null && !kingsideRook.hasMoved() &&
                board.getPiece(row, 5) == null && board.getPiece(row, 6) == null) {
            moves.add(new Position(row, 6));
        }

        Piece queensideRook = board.getPiece(row, 0);
        if (queensideRook != null && !queensideRook.hasMoved() &&
                board.getPiece(row, 1) == null && board.getPiece(row, 2) == null && board.getPiece(row, 3) == null) {
            moves.add(new Position(row, 2));
        }
    }

    private List<Position> getPawnMoves(Game game, Board board, Position position) {
        List<Position> moves = new ArrayList<>();
        Piece pawn = board.getPiece(position.row(), position.col());
        boolean isWhite = pawn.getColor() == Color.WHITE;
        int dir = isWhite ? 1 : -1;
        int row = position.row();
        int col = position.col();
        int fwd = row + dir;

        if (fwd >= 0 && fwd < 8) {
            if (board.getPiece(fwd, col) == null) {
                moves.add(new Position(fwd, col));
                int dbl = fwd + dir;
                if (!pawn.hasMoved() && dbl >= 0 && dbl < 8 && board.getPiece(dbl, col) == null)
                    moves.add(new Position(dbl, col));
            }
            addPawnCaptures(board, fwd, col, pawn.getColor(), moves);
        }

        Position ep = game.getEnPassantTarget();
        if (ep != null && ep.row() == fwd && Math.abs(ep.col() - col) == 1)
            moves.add(ep);

        return moves;
    }

    private void addPawnCaptures(Board board, int fwd, int col, Color color, List<Position> moves) {
        for (int dc : new int[]{-1, 1}) {
            int captureCol = col + dc;
            if (captureCol >= 0 && captureCol < 8) {
                Piece target = board.getPiece(fwd, captureCol);
                if (target != null && target.getColor() != color)
                    moves.add(new Position(fwd, captureCol));
            }
        }
    }

    private List<Position> slideDirection(Board board, Position position, int[][] directions) {
        List<Position> moves = new ArrayList<>();

        for (int[] direction : directions) {
            int row = position.row() + direction[0];
            int col = position.col() + direction[1];

            while (row >= 0 && row < 8 && col >= 0 && col < 8) {
                Piece target = board.getPiece(row, col);

                if (target == null) {
                    moves.add(new Position(row, col));
                } else {
                    Piece moving = board.getPiece(position.row(), position.col());
                    if (target.getColor() != moving.getColor()) {
                        moves.add(new Position(row, col));
                    }
                    break;
                }

                row += direction[0];
                col += direction[1];
            }
        }

        return moves;
    }
}
