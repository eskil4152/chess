package com.blikeng.chess.engine;

import com.blikeng.chess.model.Board;
import com.blikeng.chess.model.Position;
import com.blikeng.chess.model.piece.*;

import java.util.ArrayList;
import java.util.List;

public class MoveGenerator {
    public List<Position> getPseudoLegalMoves(Board board, Position position) {
        Piece piece = board.getPiece(position.row(), position.col());

        if (piece == null) return List.of();

        return switch (piece.getPieceType()) {
            case PieceType.ROOK -> getRookMoves(board, position);
            case PieceType.KNIGHT -> getKnightMoves(board, position);
            case PieceType.BISHOP -> getBishopMoves(board, position);
            case PieceType.QUEEN -> getQueenMoves(board, position);
            case PieceType.KING -> getKingMoves(board, position);
            case PieceType.PAWN -> getPawnMoves(board, position);
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

            if (newRow >= 0 && newRow < 8 && newCol >= 0 && newCol < 8) {
                if (
                        board.getPiece(newRow, newCol) == null ||
                        board.getPiece(newRow, newCol).getColor() != board.getPiece(position.row(), position.col()).getColor()
                ){
                    moves.add(new Position(newRow, newCol));
                }
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

        int[][] directions = {
            {-1, -1}, {-1, 0}, {-1, +1},
            {0, -1}, {0, +1},
            {+1, -1}, {+1, 0}, {+1, +1}
        };

        for (int[] direction : directions) {
            int row = position.row() + direction[0];
            int col = position.col() + direction[1];

            if ((row >= 0 && row < 8 && col >= 0 && col < 8)) {
                if (board.getPiece(row, col) == null) {
                    moves.add(new Position(row, col));
                } else {
                    Piece moving = board.getPiece(position.row(), position.col());
                    Piece target = board.getPiece(row, col);
                    if (target.getColor() != moving.getColor()) {
                        moves.add(new Position(row, col));
                    }
                }
            }
        }

        return moves;
    }

    private List<Position> getPawnMoves(Board board, Position position){
        List<Position> moves = new ArrayList<>();

        // TODO: Check if pawn can en passant

        boolean hasMoved = board.getPiece(position.row(), position.col()).hasMoved();
        boolean isWhite = board.getPiece(position.row(), position.col()).getColor() == Color.WHITE;

        if (isWhite) {
            // Walk ahead
            if (board.getPiece(position.row() - 1, position.col()) == null) moves.add(new Position(position.row() - 1, position.col()));

            // Captures
            if (
                    board.getPiece(position.row() - 1, position.col() + 1) != null &&
                    board.getPiece(position.row() - 1, position.col() + 1).getColor() != Color.WHITE
            ) moves.add(new Position(position.row() - 1, position.col() + 1));

            if (
                    board.getPiece(position.row() - 1, position.col() - 1) != null &&
                    board.getPiece(position.row() - 1, position.col() - 1).getColor() != Color.WHITE
            ) moves.add(new Position(position.row() - 1, position.col() - 1));

            if (
                    !hasMoved &&
                    board.getPiece(position.row() - 2, position.col()) == null &&
                    board.getPiece(position.row() - 1, position.col()) == null
            ) moves.add(new Position(position.row() - 2, position.col()));

        } else {
            // Walk ahead
            if (board.getPiece(position.row() + 1, position.col()) == null) moves.add(new Position(position.row() + 1, position.col()));

            // Captures
            if (
                    board.getPiece(position.row() + 1, position.col() + 1) != null &&
                    board.getPiece(position.row() + 1, position.col() + 1).getColor() != Color.BLACK
            ) moves.add(new Position(position.row() + 1, position.col() + 1));

            if (
                    board.getPiece(position.row() + 1, position.col() + -1) != null &&
                    board.getPiece(position.row() + 1, position.col() + -1).getColor() != Color.BLACK
            ) moves.add(new Position(position.row() + 1, position.col() - 1));

            // First move
            if (
                    !hasMoved &&
                    board.getPiece(position.row() + 2, position.col()) == null &&
                    board.getPiece(position.row() + 1, position.col()) == null
            ) moves.add(new Position(position.row() + 2, position.col()));
        }

        return moves;
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
