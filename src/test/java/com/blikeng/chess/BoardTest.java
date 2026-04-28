package com.blikeng.chess;

import com.blikeng.chess.model.Board;
import org.junit.jupiter.api.Test;

public class BoardTest {
    @Test
    void testBoard() {
        Board board = new Board();

        System.out.println(board);
    }
}
