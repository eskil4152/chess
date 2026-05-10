package com.blikeng.chess.unit.engine;

import com.blikeng.chess.engine.PositionMapper;
import com.blikeng.chess.model.Position;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PositionMapperTest {

    @Test
    void a1ShouldMapToRow0Col0() {
        Position p = PositionMapper.fromString("a1");
        assertThat(p.row()).isEqualTo(0);
        assertThat(p.col()).isEqualTo(0);
    }

    @Test
    void h8ShouldMapToRow7Col7() {
        Position p = PositionMapper.fromString("h8");
        assertThat(p.row()).isEqualTo(7);
        assertThat(p.col()).isEqualTo(7);
    }

    @Test
    void e4ShouldMapToRow3Col4() {
        Position p = PositionMapper.fromString("e4");
        assertThat(p.row()).isEqualTo(3);
        assertThat(p.col()).isEqualTo(4);
    }

    @Test
    void row0Col0ShouldMapToA1() {
        assertThat(PositionMapper.toString(new Position(0, 0))).isEqualTo("a1");
    }

    @Test
    void row7Col7ShouldMapToH8() {
        assertThat(PositionMapper.toString(new Position(7, 7))).isEqualTo("h8");
    }

    @Test
    void a8ShouldMapToRow7Col0() {
        Position p = PositionMapper.fromString("a8");
        assertThat(p.row()).isEqualTo(7);
        assertThat(p.col()).isEqualTo(0);
    }

    @Test
    void h1ShouldMapToRow0Col7() {
        Position p = PositionMapper.fromString("h1");
        assertThat(p.row()).isEqualTo(0);
        assertThat(p.col()).isEqualTo(7);
    }

    @Test
    void row7Col0ShouldMapToA8() {
        assertThat(PositionMapper.toString(new Position(7, 0))).isEqualTo("a8");
    }

    @Test
    void row0Col7ShouldMapToH1() {
        assertThat(PositionMapper.toString(new Position(0, 7))).isEqualTo("h1");
    }

    @Test
    void allSquaresShouldSurviveRoundTrip() {
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Position p = new Position(row, col);
                assertThat(PositionMapper.fromString(PositionMapper.toString(p))).isEqualTo(p);
            }
        }
    }
}
