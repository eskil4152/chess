package com.blikeng.chess.unit.engine.converter;

import com.blikeng.chess.engine.converter.FenConverter;
import com.blikeng.chess.model.Game;
import com.blikeng.chess.model.Position;
import com.blikeng.chess.model.piece.Bishop;
import com.blikeng.chess.model.piece.Color;
import com.blikeng.chess.model.piece.Rook;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FenConverterTest {

    private Game newGame() {
        return new Game(UUID.randomUUID(), UUID.randomUUID(), "white", UUID.randomUUID(), "black", 800, 800);
    }

    @Test
    void startingPositionProducesCorrectFen() {
        assertThat(FenConverter.toFen(newGame()))
                .isEqualTo("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    }

    @Test
    void activeColorIsWhiteOnNewGame() {
        assertThat(FenConverter.toFen(newGame())).contains(" w ");
    }

    @Test
    void activeColorIsBlackAfterWhiteMove() {
        Game game = newGame();
        game.addMove("e2e4");
        game.switchTurn();

        assertThat(FenConverter.toFen(game)).contains(" b ");
    }

    @Test
    void emptySquaresFollowedByPieceInSameRow() {
        Game game = newGame();
        game.getBoard().setPiece(0, 0, null);

        assertThat(FenConverter.toFen(game)).contains("1NBQKBNR");
    }

    @Test
    void allCastlingRightsOnNewGame() {
        assertThat(FenConverter.toFen(newGame())).contains(" KQkq ");
    }

    @Test
    void noCastlingRightsWhenKingMoved() {
        Game game = newGame();
        game.getBoard().getPiece(0, 4).setMoved();
        game.getBoard().getPiece(7, 4).setMoved();

        assertThat(FenConverter.toFen(game)).contains(" - ");
    }

    @Test
    void onlyWhiteKingsideCastlingWhenQueensideRookMoved() {
        Game game = newGame();
        game.getBoard().getPiece(0, 0).setMoved();
        game.getBoard().getPiece(7, 4).setMoved();

        assertThat(FenConverter.toFen(game)).contains(" K ");
    }

    @Test
    void onlyWhiteQueensideCastlingWhenKingsideRookMoved() {
        Game game = newGame();
        game.getBoard().getPiece(0, 7).setMoved();
        game.getBoard().getPiece(7, 4).setMoved();

        assertThat(FenConverter.toFen(game)).contains(" Q ");
    }

    @Test
    void onlyBlackKingsideCastlingWhenQueensideRookMoved() {
        Game game = newGame();
        game.getBoard().getPiece(0, 4).setMoved();
        game.getBoard().getPiece(7, 0).setMoved();

        assertThat(FenConverter.toFen(game)).contains(" k ");
    }

    @Test
    void onlyBlackQueensideCastlingWhenKingsideRookMoved() {
        Game game = newGame();
        game.getBoard().getPiece(0, 4).setMoved();
        game.getBoard().getPiece(7, 7).setMoved();

        assertThat(FenConverter.toFen(game)).contains(" q ");
    }

    @Test
    void noCastlingRightsWhenWrongColorRookOnSquare() {
        Game game = newGame();
        game.getBoard().setPiece(0, 7, new Rook(Color.BLACK));
        game.getBoard().setPiece(7, 7, new Rook(Color.WHITE));

        assertThat(FenConverter.toFen(game)).contains(" Qq ");
    }

    @Test
    void noCastlingRightsWhenWrongPieceOnRookSquare() {
        Game game = newGame();
        game.getBoard().setPiece(0, 7, new Bishop(Color.WHITE));
        game.getBoard().setPiece(7, 7, new Bishop(Color.BLACK));

        assertThat(FenConverter.toFen(game)).contains(" Qq ");
    }

    @Test
    void noCastlingRightsWhenRookCaptured() {
        Game game = newGame();
        game.getBoard().setPiece(0, 7, null);
        game.getBoard().setPiece(0, 0, null);
        game.getBoard().setPiece(7, 7, null);
        game.getBoard().setPiece(7, 0, null);

        assertThat(FenConverter.toFen(game)).contains(" - ");
    }

    @Test
    void noEnPassantOnNewGame() {
        assertThat(FenConverter.toFen(newGame())).contains("- 0 1");
    }

    @Test
    void enPassantTargetSquareIsIncluded() {
        Game game = newGame();
        game.setEnPassantTarget(new Position(2, 4));

        assertThat(FenConverter.toFen(game)).contains(" e3 ");
    }

    @Test
    void halfMoveClockIsIncluded() {
        Game game = newGame();
        game.setHalfMoveClock(5);

        assertThat(FenConverter.toFen(game)).contains(" 5 ");
    }

    @Test
    void fullMoveNumberStartsAtOne() {
        assertThat(FenConverter.toFen(newGame())).endsWith("0 1");
    }

    @Test
    void fullMoveNumberIncrementsAfterBothPlayersMove() {
        Game game = newGame();
        game.addMove("e2e4");
        game.addMove("e7e5");

        assertThat(FenConverter.toFen(game)).endsWith("0 2");
    }
}