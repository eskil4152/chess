package com.blikeng.chess.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

@Getter
public class Game {
    public Game(String whiteId, String whiteUsername, String blackId, String blackUsername) {
        this.whiteId = whiteId;
        this.whiteUsername = whiteUsername;
        this.blackId = blackId;
        this.blackUsername = blackUsername;
    }

    private final UUID id = UUID.randomUUID();

    private final ReentrantLock lock = new ReentrantLock();

    private String whiteId;
    private String whiteUsername;

    private String blackId;
    private String blackUsername;

    @Setter
    private Position whiteKingPosition;

    @Setter
    private Position blackKingPosition;

    private Board board;
    private boolean isWhiteTurn;
    private List<MoveRecord> moves;

    @Setter
    private GameStatus status = GameStatus.ONGOING;

    @Setter
    private Position enPassantTarget;

    public Game() {
        this.board = new Board();
    }

    public void switchTurn() {
        isWhiteTurn = !isWhiteTurn;
    }

    public ReentrantLock lockGame(){
        return lock;
    }

    public void addMove(MoveRecord moveRecord){
        moves.add(moveRecord);
    }
}