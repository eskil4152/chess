package com.blikeng.chess.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

@Getter
public class Game {
    public Game(UUID id, UUID whiteId, String whiteUsername, UUID blackId, String blackUsername) {
        this.id = id;
        this.whiteId = whiteId;
        this.whiteUsername = whiteUsername;
        this.blackId = blackId;
        this.blackUsername = blackUsername;
        this.board = new Board();
    }

    public Game(Game other) {
        this.id = other.id;
        this.whiteId = other.whiteId;
        this.whiteUsername = other.whiteUsername;
        this.blackId = other.blackId;
        this.blackUsername = other.blackUsername;
        this.board = new Board(other.board);
        this.isWhiteTurn = other.isWhiteTurn;
        this.whiteKingPosition = other.whiteKingPosition;
        this.blackKingPosition = other.blackKingPosition;
        this.enPassantTarget = other.enPassantTarget;
        this.status = other.status;
    }

    private final UUID id;

    private final ReentrantLock lock = new ReentrantLock();

    private final UUID whiteId;
    private final String whiteUsername;

    private final UUID blackId;
    private final String blackUsername;

    @Setter
    private Position whiteKingPosition;

    @Setter
    private Position blackKingPosition;

    private final Board board;
    private boolean isWhiteTurn = true;
    private final List<String> moves = new ArrayList<>();

    @Setter
    private GameStatus status = GameStatus.ONGOING;

    @Setter
    private Position enPassantTarget;

    public void switchTurn() {
        isWhiteTurn = !isWhiteTurn;
    }

    public ReentrantLock lockGame(){
        return lock;
    }

    public void addMove(String move){
        moves.add(move);
    }
}