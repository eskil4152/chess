package com.blikeng.chess.model;

public class Game {
    public Board board;
    public boolean isWhiteTurn = true;
    public GameStatus status = GameStatus.ONGOING;

    public Game() {
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