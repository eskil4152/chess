package com.blikeng.chess.unit.model;

import com.blikeng.chess.model.Game;
import com.blikeng.chess.model.GameStatus;
import com.blikeng.chess.model.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.assertThat;

class GameTest {

    private Game game;
    private UUID whiteId;
    private UUID blackId;

    @BeforeEach
    void setup() {
        whiteId = UUID.randomUUID();
        blackId = UUID.randomUUID();
        game = new Game(UUID.randomUUID(), whiteId, "white", blackId, "black", 800, 800);
    }

    @Test
    void shouldCorrectlySetConstructorFields() {
        assertThat(game.getId()).isNotNull();
        assertThat(game.getWhiteId()).isEqualTo(whiteId);
        assertThat(game.getBlackId()).isEqualTo(blackId);
        assertThat(game.getWhiteUsername()).isEqualTo("white");
        assertThat(game.getBlackUsername()).isEqualTo("black");
        assertThat(game.isWhiteTurn()).isTrue();
        assertThat(game.getStatus()).isEqualTo(GameStatus.ONGOING);
        assertThat(game.getBoard()).isNotNull();
        assertThat(game.getEnPassantTarget()).isNull();
    }

    @Test
    void shouldToggleWhiteTurn() {
        assertThat(game.isWhiteTurn()).isTrue();
        game.switchTurn();
        assertThat(game.isWhiteTurn()).isFalse();
        game.switchTurn();
        assertThat(game.isWhiteTurn()).isTrue();
    }

    @Test
    void lockGameShouldReturnLock() {
        ReentrantLock lock = game.lockGame();
        assertThat(lock).isNotNull();
        assertThat(lock).isSameAs(game.lockGame());
    }

    @Test
    void shouldStoreKingPositions() {
        game.setWhiteKingPosition(new Position(7, 4));
        game.setBlackKingPosition(new Position(0, 4));
        assertThat(game.getWhiteKingPosition()).isEqualTo(new Position(7, 4));
        assertThat(game.getBlackKingPosition()).isEqualTo(new Position(0, 4));
    }

    @Test
    void shouldSetEnPassantTarget() {
        Position target = new Position(5, 3);
        game.setEnPassantTarget(target);
        assertThat(game.getEnPassantTarget()).isEqualTo(target);
        game.setEnPassantTarget(null);
        assertThat(game.getEnPassantTarget()).isNull();
    }

    @Test
    void shouldUpdateGameStatus() {
        game.setStatus(GameStatus.WHITE_WIN);
        assertThat(game.getStatus()).isEqualTo(GameStatus.WHITE_WIN);
    }

    @Test
    void shouldAddMoveToMovesList() {
        assertThat(game.getMoves()).isEmpty();

        game.addMove("e2e4");
        game.addMove("e7e5");
        assertThat(game.getMoves()).containsExactly("e2e4", "e7e5");
    }
}
