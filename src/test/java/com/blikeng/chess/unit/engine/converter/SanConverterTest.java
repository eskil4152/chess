package com.blikeng.chess.unit.engine.converter;

import com.blikeng.chess.engine.converter.SanConverter;
import com.blikeng.chess.model.Game;
import com.blikeng.chess.model.Move;
import com.blikeng.chess.model.Position;
import com.blikeng.chess.model.piece.*;
import com.blikeng.chess.model.timecontrol.TimeControl;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SanConverterTest {

    private Game newGame() {
        Game game = new Game(UUID.randomUUID(), UUID.randomUUID(), "white", UUID.randomUUID(), "black", 800, 800, TimeControl.BLITZ_3_0 , 1000, 1000, 0);
        game.setWhiteKingPosition(new Position(7, 4));
        game.setBlackKingPosition(new Position(0, 4));
        return game;
    }

    private void clearAll(Game game) {
        for (int row = 0; row < 8; row++)
            for (int col = 0; col < 8; col++)
                game.getBoard().setPiece(row, col, null);
    }

    private void place(Game game, Piece piece, int row, int col) {
        game.getBoard().setPiece(row, col, piece);
    }

    @Test
    void pawnForwardMove() {
        Game game = newGame();
        assertThat(SanConverter.toSan(game, new Move(new Position(6, 4), new Position(4, 4), null))).isEqualTo("e5");
    }

    @Test
    void pieceMoveNoCapture() {
        Game game = newGame();
        assertThat(SanConverter.toSan(game, new Move(new Position(7, 6), new Position(5, 5), null))).isEqualTo("Nf6");
    }

    @Test
    void pawnCapture() {
        Game game = newGame();
        clearAll(game);
        place(game, new King(Color.WHITE), 7, 4);
        place(game, new King(Color.BLACK), 0, 4);
        place(game, new Pawn(Color.WHITE), 4, 4);
        place(game, new Pawn(Color.BLACK), 3, 3);
        game.setWhiteKingPosition(new Position(7, 4));
        game.setBlackKingPosition(new Position(0, 4));
        assertThat(SanConverter.toSan(game, new Move(new Position(4, 4), new Position(3, 3), null))).isEqualTo("exd4");
    }

    @Test
    void pieceCaptureWithX() {
        Game game = newGame();
        clearAll(game);
        place(game, new King(Color.WHITE), 7, 4);
        place(game, new King(Color.BLACK), 0, 4);
        place(game, new Knight(Color.WHITE), 5, 5);
        place(game, new Pawn(Color.BLACK), 3, 4);
        game.setWhiteKingPosition(new Position(7, 4));
        game.setBlackKingPosition(new Position(0, 4));
        assertThat(SanConverter.toSan(game, new Move(new Position(5, 5), new Position(3, 4), null))).isEqualTo("Nxe4");
    }

    @Test
    void pawnPromotion() {
        Game game = newGame();
        clearAll(game);
        place(game, new King(Color.WHITE), 7, 4);
        place(game, new King(Color.BLACK), 0, 7);
        place(game, new Pawn(Color.WHITE), 1, 4);
        game.setWhiteKingPosition(new Position(7, 4));
        game.setBlackKingPosition(new Position(0, 7));
        assertThat(SanConverter.toSan(game, new Move(new Position(1, 4), new Position(0, 4), PieceType.QUEEN))).isEqualTo("e1=Q");
    }

    @Test
    void pawnCapturePromotion() {
        Game game = newGame();
        clearAll(game);
        place(game, new King(Color.WHITE), 7, 4);
        place(game, new King(Color.BLACK), 0, 7);
        place(game, new Pawn(Color.WHITE), 1, 4);
        place(game, new Rook(Color.BLACK), 0, 5);
        game.setWhiteKingPosition(new Position(7, 4));
        game.setBlackKingPosition(new Position(0, 7));
        assertThat(SanConverter.toSan(game, new Move(new Position(1, 4), new Position(0, 5), PieceType.QUEEN))).isEqualTo("exf1=Q");
    }

    @Test
    void kingsideCastle() {
        Game game = newGame();
        game.getBoard().setPiece(7, 5, null);
        game.getBoard().setPiece(7, 6, null);
        assertThat(SanConverter.toSan(game, new Move(new Position(7, 4), new Position(7, 6), null))).isEqualTo("O-O");
    }

    @Test
    void queensideCastle() {
        Game game = newGame();
        game.getBoard().setPiece(7, 1, null);
        game.getBoard().setPiece(7, 2, null);
        game.getBoard().setPiece(7, 3, null);
        assertThat(SanConverter.toSan(game, new Move(new Position(7, 4), new Position(7, 2), null))).isEqualTo("O-O-O");
    }

    @Test
    void checkAppendsPlus() {
        Game game = newGame();
        clearAll(game);
        place(game, new King(Color.WHITE), 7, 4);
        place(game, new Queen(Color.WHITE), 7, 3);
        place(game, new King(Color.BLACK), 0, 4);
        place(game, new Rook(Color.BLACK), 0, 0);
        game.setWhiteKingPosition(new Position(7, 4));
        game.setBlackKingPosition(new Position(0, 4));
        assertThat(SanConverter.toSan(game, new Move(new Position(7, 3), new Position(0, 3), null))).isEqualTo("Qd1+");
    }

    @Test
    void kingNonCastleMove() {
        Game game = newGame();
        clearAll(game);
        place(game, new King(Color.WHITE), 5, 4);
        place(game, new King(Color.BLACK), 0, 0);
        game.setWhiteKingPosition(new Position(5, 4));
        game.setBlackKingPosition(new Position(0, 0));
        assertThat(SanConverter.toSan(game, new Move(new Position(5, 4), new Position(4, 4), null))).isEqualTo("Ke5");
    }

    @Test
    void blackWinCheckmateAppendsHash() {
        Game game = newGame();
        clearAll(game);
        place(game, new King(Color.WHITE), 0, 7);
        place(game, new King(Color.BLACK), 7, 7);
        place(game, new Queen(Color.BLACK), 2, 6);
        place(game, new Rook(Color.BLACK), 0, 0);
        game.setWhiteKingPosition(new Position(0, 7));
        game.setBlackKingPosition(new Position(7, 7));
        game.switchTurn();
        assertThat(SanConverter.toSan(game, new Move(new Position(2, 6), new Position(0, 6), null))).isEqualTo("Qg1#");
    }

    @Test
    void stalemateReturnsSanWithoutSuffix() {
        Game game = newGame();
        clearAll(game);
        place(game, new King(Color.WHITE), 5, 0);
        place(game, new King(Color.BLACK), 0, 7);
        place(game, new Queen(Color.WHITE), 2, 0);
        game.setWhiteKingPosition(new Position(5, 0));
        game.setBlackKingPosition(new Position(0, 7));
        assertThat(SanConverter.toSan(game, new Move(new Position(2, 0), new Position(2, 6), null))).isEqualTo("Qg3");
    }

    @Test
    void pieceDisambiguationByRank() {
        Game game = newGame();
        clearAll(game);
        place(game, new King(Color.WHITE), 7, 4);
        place(game, new King(Color.BLACK), 0, 4);
        place(game, new Rook(Color.WHITE), 5, 3);
        place(game, new Rook(Color.WHITE), 3, 7);
        game.setWhiteKingPosition(new Position(7, 4));
        game.setBlackKingPosition(new Position(0, 4));
        assertThat(SanConverter.toSan(game, new Move(new Position(5, 3), new Position(3, 3), null))).isEqualTo("R4d4");
    }

    @Test
    void pieceDisambiguationByFile() {
        Game game = newGame();
        clearAll(game);
        place(game, new King(Color.WHITE), 7, 4);
        place(game, new King(Color.BLACK), 0, 4);
        place(game, new Rook(Color.WHITE), 3, 0);
        place(game, new Rook(Color.WHITE), 7, 5);
        game.setWhiteKingPosition(new Position(7, 4));
        game.setBlackKingPosition(new Position(0, 4));
        assertThat(SanConverter.toSan(game, new Move(new Position(3, 0), new Position(3, 5), null))).isEqualTo("Raf4");
    }

    @Test
    void pieceDisambiguationByFileWhenNeitherRowNorColMatch() {
        Game game = newGame();
        clearAll(game);
        place(game, new King(Color.WHITE), 7, 4);
        place(game, new King(Color.BLACK), 0, 4);
        place(game, new Knight(Color.WHITE), 5, 5);
        place(game, new Knight(Color.WHITE), 1, 3);
        game.setWhiteKingPosition(new Position(7, 4));
        game.setBlackKingPosition(new Position(0, 4));
        assertThat(SanConverter.toSan(game, new Move(new Position(5, 5), new Position(3, 4), null))).isEqualTo("Nfe4");
    }

    @Test
    void noAmbiguityWhenOtherPieceIsOnSameColumn() {
        Game game = newGame();
        clearAll(game);
        place(game, new King(Color.WHITE), 7, 4);
        place(game, new King(Color.BLACK), 0, 4);
        place(game, new Rook(Color.WHITE), 5, 3);
        place(game, new Rook(Color.WHITE), 3, 3);
        game.setWhiteKingPosition(new Position(7, 4));
        game.setBlackKingPosition(new Position(0, 4));
        assertThat(SanConverter.toSan(game, new Move(new Position(5, 3), new Position(5, 5), null))).isEqualTo("Rf6");
    }

    @Test
    void noDisambiguationWhenAmbiguousPieceCannotReachTarget() {
        Game game = newGame();
        clearAll(game);
        place(game, new King(Color.WHITE), 7, 4);
        place(game, new King(Color.BLACK), 0, 4);
        place(game, new Knight(Color.WHITE), 6, 0);
        place(game, new Knight(Color.WHITE), 4, 7);
        game.setWhiteKingPosition(new Position(7, 4));
        game.setBlackKingPosition(new Position(0, 4));
        assertThat(SanConverter.toSan(game, new Move(new Position(6, 0), new Position(4, 1), null))).isEqualTo("Nb5");
    }

    @Test
    void checkmateAppendsHash() {
        Game game = newGame();
        clearAll(game);
        place(game, new King(Color.WHITE), 7, 4);
        place(game, new Queen(Color.WHITE), 3, 7);
        place(game, new Bishop(Color.WHITE), 4, 2);
        place(game, new King(Color.BLACK), 0, 4);
        place(game, new Queen(Color.BLACK), 0, 3);
        place(game, new Bishop(Color.BLACK), 0, 2);
        place(game, new Knight(Color.BLACK), 0, 1);
        place(game, new Rook(Color.BLACK), 0, 0);
        place(game, new Bishop(Color.BLACK), 0, 5);
        place(game, new Knight(Color.BLACK), 0, 6);
        place(game, new Rook(Color.BLACK), 0, 7);
        place(game, new Pawn(Color.BLACK), 1, 3);
        place(game, new Pawn(Color.BLACK), 1, 5);
        place(game, new Pawn(Color.BLACK), 1, 6);
        place(game, new Pawn(Color.BLACK), 1, 7);
        place(game, new Pawn(Color.BLACK), 3, 4);
        game.setWhiteKingPosition(new Position(7, 4));
        game.setBlackKingPosition(new Position(0, 4));
        assertThat(SanConverter.toSan(game, new Move(new Position(3, 7), new Position(1, 5), null))).isEqualTo("Qxf2#");
    }
}
