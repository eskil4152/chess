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

    private final UUID id;

    private final ReentrantLock lock = new ReentrantLock();

    private UUID whiteId;
    private String whiteUsername;

    private UUID blackId;
    private String blackUsername;

    @Setter
    private Position whiteKingPosition;

    @Setter
    private Position blackKingPosition;

    private Board board;
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