package com.blikeng.chess.entity;

import com.blikeng.chess.model.Board;
import com.blikeng.chess.model.GameStatus;

import java.util.UUID;

public class GameEntity {
    private final UUID id = UUID.randomUUID();

    private String whitePlayer;
    private String blackPlayer;

    private Board board;
    private boolean isWhiteTurn = true;
    private GameStatus status = GameStatus.ONGOING;

    public GameEntity() {
        this.board = new Board();
    }

    public Board getBoard() {
        return board;
    }

    public boolean isWhiteTurn() {
        return isWhiteTurn;
    }

    public void switchTurn() {
        isWhiteTurn = !isWhiteTurn;
    }
}