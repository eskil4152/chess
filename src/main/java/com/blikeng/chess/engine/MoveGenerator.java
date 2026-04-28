package com.blikeng.chess.engine;

import com.blikeng.chess.model.Board;
import com.blikeng.chess.model.Position;
import com.blikeng.chess.model.piece.Color;

import java.util.ArrayList;
import java.util.List;

public class MoveGenerator {
    public List<Position> getRookMoves(Board board, Position position) {
        List<Position> moves = new ArrayList<>();

        int[][] directions = {
            {-1, 0}, {+1, 0},
            {0, -1}, {0, +1}
        };

        return slideDirection(position, moves, directions);
    }

    public List<Position> getKnightMoves(Board board, Position position) {
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
                moves.add(new Position(newRow, newCol));
            }
        }

        return moves;
    }

    public List<Position> getBishopMoves(Board board, Position position) {
        List<Position> moves = new ArrayList<>();

        int[][] directions = {
                {-1, -1}, {-1, +1},
                {+1, -1}, {+1, +1}
        };

        return slideDirection(position, moves, directions);
    }

    public List<Position> getQueenMoves(Board board, Position position) {
        List<Position> moves = new ArrayList<>();

        int[][] directions = {
            {-1, -1}, {-1, +1},
            {+1, -1}, {+1, +1},
            {-1, 0}, {+1, 0},
            {0, -1}, {0, +1}
        };

        return slideDirection(position, moves, directions);
    }

    public List<Position> getKingMoves(Board board, Position position) {
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
                moves.add(new Position(row, col));
            }
        }

        return moves;
    }

    public List<Position> getPawnMoves(Board board, Position position){
        List<Position> moves = new ArrayList<>();

        // TODO: Check if pawn can en passant

        boolean hasMoved = board.getPiece(position.row(), position.col()).hasMoved();
        boolean isWhite = board.getPiece(position.row(), position.col()).getColor() == Color.WHITE;

        if (isWhite) {
            // Walk ahead
            moves.add(new Position(position.row() - 1, position.col()));

            // Captures
            moves.add(new Position(position.row() - 1, position.col() + 1));
            moves.add(new Position(position.row() - 1, position.col() - 1));

            if (!hasMoved) {
                moves.add(new Position(position.row() - 2, position.col()));
            }
        } else {
            // Walk ahead
            moves.add(new Position(position.row() + 1, position.col()));

            // Captures
            moves.add(new Position(position.row() + 1, position.col() + 1));
            moves.add(new Position(position.row() + 1, position.col() - 1));

            if (!hasMoved) {
                moves.add(new Position(position.row() + 2, position.col()));
            }
        }

        return moves;
    }

    private List<Position> slideDirection(Position position, List<Position> moves, int[][] directions) {
        for (int[] direction : directions) {
            int row = position.row() + direction[0];
            int col = position.col() + direction[1];

            while (row >= 0 && row < 8 && col >= 0 && col < 8) {
                moves.add(new Position(row, col));
                row += direction[0];
                col += direction[1];
            }
        }

        return moves;
    }
}
