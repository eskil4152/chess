package com.blikeng.chess.entity;

import com.blikeng.chess.model.Board;
import com.blikeng.chess.model.GameStatus;
import lombok.Getter;

import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

@Getter
public class GameEntity {
    public GameEntity(String whiteId, String whiteUsername, String blackId, String blackUsername) {
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

    private Board board;
    private boolean isWhiteTurn;
    private GameStatus status = GameStatus.ONGOING;

    public GameEntity() {
        this.board = new Board();
    }

    public void switchTurn() {
        isWhiteTurn = !isWhiteTurn;
    }

    public ReentrantLock lockGame(){
        return lock;
    }
}