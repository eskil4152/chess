package com.blikeng.chess.engine;

import com.blikeng.chess.model.Board;
import com.blikeng.chess.model.Game;
import com.blikeng.chess.model.Position;
import com.blikeng.chess.model.piece.Color;
import com.blikeng.chess.model.piece.Piece;
import com.blikeng.chess.model.piece.PieceType;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates the pseudo-legal moves for a single piece.
 *
 * <p>Uses lists of directions which are looped for relevant pieces. Castling moves are handled by checking
 * relevant squares on the board. Illegal castling (through check) is handled by {@link MoveExecutor}.
 *
 * <p>Pawn moves are checked with en passant and capturing.
 * Board edges are considered on pawn captures, since checking positions outside the board for pieces results in out-of-bounds.
 */
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

        Piece sourcePiece = board.getPiece(position.row(), position.col());
        for (int[] direction : directions) {
            int row = position.row() + direction[0];
            int col = position.col() + direction[1];

            if (row >= 0 && row < 8 && col >= 0 && col < 8) {
                Piece target = board.getPiece(row, col);
                if (target == null || target.getColor() != sourcePiece.getColor()) {
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
        int direction = isWhite ? 1 : -1;
        int row = position.row();
        int col = position.col();
        int nextRow = row + direction;

        if (nextRow >= 0 && nextRow < 8) {
            if (board.getPiece(nextRow, col) == null) {
                moves.add(new Position(nextRow, col));
                int doubleRow = nextRow + direction;
                if (!pawn.hasMoved() && doubleRow >= 0 && doubleRow < 8 && board.getPiece(doubleRow, col) == null)
                    moves.add(new Position(doubleRow, col));
            }
            addPawnCaptures(board, nextRow, col, pawn.getColor(), moves);
        }

        Position enPassantTarget = game.getEnPassantTarget();
        if (enPassantTarget != null && enPassantTarget.row() == nextRow && Math.abs(enPassantTarget.col() - col) == 1)
            moves.add(enPassantTarget);

        return moves;
    }

    private void addPawnCaptures(Board board, int nextRow, int col, Color color, List<Position> moves) {
        for (int colOffset : new int[]{-1, 1}) {
            int captureCol = col + colOffset;
            if (captureCol >= 0 && captureCol < 8) {
                Piece target = board.getPiece(nextRow, captureCol);
                if (target != null && target.getColor() != color)
                    moves.add(new Position(nextRow, captureCol));
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
                    Piece sourcePiece = board.getPiece(position.row(), position.col());
                    if (target.getColor() != sourcePiece.getColor()) {
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
