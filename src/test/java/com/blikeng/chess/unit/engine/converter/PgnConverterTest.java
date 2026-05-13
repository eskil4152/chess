package com.blikeng.chess.unit.engine.converter;

import com.blikeng.chess.engine.converter.PgnConverter;
import com.blikeng.chess.model.Game;
import com.blikeng.chess.model.Position;
import com.blikeng.chess.model.timecontrol.TimeControl;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PgnConverterTest {

    private Game newGame() {
        Game game = new Game(UUID.randomUUID(), UUID.randomUUID(), "white", UUID.randomUUID(), "black", 800, 800, TimeControl.BLITZ_3_0, 1000, 1000, 1000);
        game.setWhiteKingPosition(new Position(0, 4));
        game.setBlackKingPosition(new Position(7, 4));
        return game;
    }

    @Test
    void noMovesReturnsEmptyString() {
        assertThat(PgnConverter.toPgn(newGame())).isEmpty();
    }

    @Test
    void singleWhiteMoveFormatsWithMoveNumber() {
        Game game = newGame();
        game.addMove("e2e4");
        assertThat(PgnConverter.toPgn(game)).isEqualTo("1. e4");
    }

    @Test
    void oneFullMoveFormatsCorrectly() {
        Game game = newGame();
        game.addMove("e2e4");
        game.addMove("e7e5");
        assertThat(PgnConverter.toPgn(game)).isEqualTo("1. e4 e5");
    }

    @Test
    void multipleMovesFormatCorrectly() {
        Game game = newGame();
        game.addMove("e2e4");
        game.addMove("e7e5");
        game.addMove("g1f3");
        game.addMove("b8c6");
        assertThat(PgnConverter.toPgn(game)).isEqualTo("1. e4 e5 2. Nf3 Nc6");
    }

    @Test
    void originalGameIsNotModifiedByToPgn() {
        Game game = newGame();
        game.addMove("e2e4");
        PgnConverter.toPgn(game);
        assertThat(game.getBoard().getPiece(1, 4)).isNotNull();
    }
}
