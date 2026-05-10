package com.blikeng.chess.engine.converter;

import java.util.List;

import com.blikeng.chess.engine.MoveExecutor;
import com.blikeng.chess.engine.MoveGenerator;
import com.blikeng.chess.engine.PositionMapper;
import com.blikeng.chess.engine.SquareAttacked;
import com.blikeng.chess.model.Game;
import com.blikeng.chess.model.Move;
import com.blikeng.chess.model.Board;
import com.blikeng.chess.model.GameStatus;
import com.blikeng.chess.model.Position;
import com.blikeng.chess.model.piece.Color;
import com.blikeng.chess.model.piece.Piece;
import com.blikeng.chess.model.piece.PieceType;

public class SanConverter {
    private SanConverter() {}

    private static final MoveGenerator moveGenerator = new MoveGenerator();
    private static final MoveExecutor moveExecutor = new MoveExecutor();
    private final static SquareAttacked squareAttacked = new SquareAttacked(moveGenerator);

    public static String toSan(Game game, Move move) {
        StringBuilder sb = new StringBuilder();
        Board board = game.getBoard();

        Piece piece = board.getPiece(move.from().row(), move.from().col());

        boolean isCastle = piece.getPieceType() == PieceType.KING && (move.to().col() - move.from().col() == 2 || move.to().col() - move.from().col() == -2);

        boolean isWhiteMove = board.getPiece(move.from().row(), move.from().col()).getColor() == Color.WHITE;

        if (isCastle) {
            handleCastle(sb, move);
        } else if (piece.getPieceType() == PieceType.PAWN){
            handlePawn(sb, move);
        } else {
            handlePiece(sb, game, move);
        }

        Game copy = new Game(game);
        GameStatus status = moveExecutor.performMove(copy, move);
        if (status == GameStatus.WHITE_WIN || status == GameStatus.BLACK_WIN) {
            sb.append('#');

            return sb.toString().trim();
        } else if (status == GameStatus.DRAW) {
            return sb.toString().trim();
        } else if (squareAttacked.isInCheck(copy, isWhiteMove ? Color.BLACK : Color.WHITE)) {
            sb.append('+');
        }

        return sb.toString().trim();
    }

    private static void handleCastle(StringBuilder stringBuilder, Move move){
        if (move.to().col() == 6) {
            stringBuilder.append("O-O");
        } else {
            stringBuilder.append("O-O-O");
        }
    }

    private static void handlePawn(StringBuilder stringBuilder, Move move){
        if (move.from().col() == move.to().col()) {
            stringBuilder
                    .append(PositionMapper.toString(move.to()));
        } else {
            stringBuilder
                    .append(PositionMapper.toString(move.from()).charAt(0))
                    .append('x')
                    .append(PositionMapper.toString(move.to()));
        }

        if (move.promotionPiece() != null) {
            stringBuilder.append('=').append(PieceType.toChar(move.promotionPiece()));
        }
    }

    private static void handlePiece(StringBuilder stringBuilder, Game game, Move move){
        Piece targetPiece = game.getBoard().getPiece(move.to().row(), move.to().col());

        stringBuilder
                .append(PieceType.toChar(game.getBoard().getPiece(move.from().row(), move.from().col()).getPieceType()));

        handleAmbiguousMove(stringBuilder, game, move);

        if (targetPiece != null) stringBuilder.append("x");

        stringBuilder.append(PositionMapper.toString(move.to()));
    }

    private static void handleAmbiguousMove(StringBuilder stringBuilder, Game game, Move move){
        Piece piece = game.getBoard().getPiece(move.from().row(), move.from().col());
        PieceType type = piece.getPieceType();

        Position ambiguousPiecePosition = null;

        for (int row = 0; row < 8; row++){
            for (int col = 0; col < 8; col++){
                if (
                        game.getBoard().getPiece(row, col) != null &&
                        game.getBoard().getPiece(row, col).getPieceType() == type &&
                        game.getBoard().getPiece(row, col).getColor() == piece.getColor() &&
                        row != move.from().row() && col != move.from().col()
                ){
                    ambiguousPiecePosition = new Position(row, col);
                    break;
                }
            }
        }

        if (ambiguousPiecePosition != null){
            List<Position> ambiguousPieceMoves = moveGenerator.getPseudoLegalMoves(
                    game,
                    game.getBoard(),
                    new Position(
                            ambiguousPiecePosition.row(),
                            ambiguousPiecePosition.col()
                    ));

            boolean hasSameMove = ambiguousPieceMoves.contains(move.to());

            if (hasSameMove) {
                if (ambiguousPiecePosition.row() == move.to().row()){
                    stringBuilder.append((char)('1' + (8 - move.from().row())));
                } else if (ambiguousPiecePosition.col() == move.to().col()){
                    stringBuilder.append((char)('a' + move.from().col()));
                } else {
                    stringBuilder.append((char) ('a' + move.from().col()));
                }
            }
        }
    }
}

// Call from 'toPgn' function. Called on every move.

// Done:
// Check if move is pawn move (e4 / e5 etc.) or piece move (Nf3 / Bc4 etc.)
// Check if move is castle (O-O / O-O-O)
// Check if move is promotion (f8=Q)
// Check if move is capture promotion (fxe8=Q)
// Check if move is capture (piece x target, e.g. Nxf3 or exf3)
// Check if move is check (Qa8+)
// Check if move is checkmate (Qa8#)
// Check if move is stalemate
// Check if same piece exists on same column or row (e.g. Nef3 / N3f6)
// Check if same piece exists on same column or row on capture (e.g. Nexf3 / N3xf6)

// Needs:
