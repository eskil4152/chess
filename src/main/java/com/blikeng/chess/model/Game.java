package com.blikeng.chess.model;

import com.blikeng.chess.model.timecontrol.TimeControl;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

@Getter
public class Game {
    public Game(
        UUID id,
        UUID whiteId,
        String whiteUsername,
        UUID blackId,
        String blackUsername,
        int whiteElo,
        int blackElo,
        boolean botGame,
        TimeControl timeControl
    ) {
        this.id = id;
        this.whiteId = whiteId;
        this.whiteUsername = whiteUsername;
        this.blackId = blackId;
        this.blackUsername = blackUsername;
        this.whiteElo = whiteElo;
        this.blackElo = blackElo;
        this.botGame = botGame;
        this.board = new Board();
        this.timeControl = timeControl;
    }

    public Game(Game other) {
        this.id = other.id;
        this.whiteId = other.whiteId;
        this.whiteUsername = other.whiteUsername;
        this.blackId = other.blackId;
        this.blackUsername = other.blackUsername;
        this.botGame = other.botGame;
        this.board = new Board(other.board);
        this.isWhiteTurn = other.isWhiteTurn;
        this.whiteKingPosition = other.whiteKingPosition;
        this.blackKingPosition = other.blackKingPosition;
        this.enPassantTarget = other.enPassantTarget;
        this.status = other.status;
        this.whiteElo = other.whiteElo;
        this.blackElo = other.blackElo;
        this.whiteDraw = other.whiteDraw;
        this.blackDraw = other.blackDraw;
        this.moves.addAll(other.moves);
        this.halfMoveClock = other.halfMoveClock;
        this.endedBy = other.endedBy;
        this.positionHistory = new HashMap<>(other.positionHistory);
        this.timeControl = other.timeControl;
    }

    private final UUID id;

    private final ReentrantLock lock = new ReentrantLock();

    private final UUID whiteId;
    private final String whiteUsername;

    private final UUID blackId;
    private final String blackUsername;

    @Setter
    private boolean whiteDraw = false;
    @Setter
    private boolean blackDraw = false;

    private final int whiteElo;
    private final int blackElo;
    private final boolean botGame;

    @Setter
    private Position whiteKingPosition = new Position(0, 4);

    @Setter
    private Position blackKingPosition = new Position(7, 4);

    private final Board board;
    private boolean isWhiteTurn = true;
    private final List<String> moves = new ArrayList<>();

    @Setter
    private GameStatus status = GameStatus.ONGOING;

    @Setter
    private int halfMoveClock = 0;

    @Setter
    private EndedBy endedBy = null;

    @Setter
    private HashMap<String, Integer> positionHistory = new HashMap<>();

    @Setter
    private Position enPassantTarget;

    private final TimeControl timeControl;

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