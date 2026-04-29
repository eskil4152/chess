package com.blikeng.chess.service;

import com.blikeng.chess.dto.GameDTO;
import com.blikeng.chess.dto.MoveDTO;
import com.blikeng.chess.engine.MoveExecutor;
import com.blikeng.chess.engine.PositionMapper;
import com.blikeng.chess.model.Game;
import com.blikeng.chess.exception.ErrorTypes.GameNotFoundException;
import com.blikeng.chess.exception.ErrorTypes.InvalidUserException;
import com.blikeng.chess.model.GameStatus;
import com.blikeng.chess.model.Move;
import com.blikeng.chess.model.Position;
import com.blikeng.chess.security.JwtPrincipal;
import com.blikeng.chess.security.JwtService;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class GameService {
    private final JwtService jwtService;
    private final MoveExecutor moveExecutor = new MoveExecutor();

    public GameService(
            JwtService jwtService
    ) {
        this.jwtService = jwtService;
    }

    private final ConcurrentHashMap<UUID, Game> games = new ConcurrentHashMap<UUID, Game>();

    public void beginGame(GameDTO gameDTO){
        Game game = new Game(gameDTO.whiteId(), gameDTO.whiteUsername(), gameDTO.blackId(), gameDTO.blackUsername());
        game.setWhiteKingPosition(new Position(7, 4));
        game.setBlackKingPosition(new Position(0, 4));

        games.put(game.getId(), game);
    }

    public void makeMove(MoveDTO moveDTO){
        Optional<Game> optionalGame = getGame(moveDTO.gameId());
        Game game;

        if (optionalGame.isEmpty()) {
            throw new GameNotFoundException();
        } else {
            game = optionalGame.get();
        }

        ReentrantLock lock = game.lockGame();
        lock.lock();

        try {
            JwtPrincipal user;
            try {
                user = jwtService.getCurrentUser();
            } catch (IllegalArgumentException e) {
                throw new InvalidUserException();
            }

            UUID userId = user.userId();

            if (!isUserTurn(game, userId)){
                return;
            }

            GameStatus gameStatus = moveExecutor.performMove(
                    game,
                    new Move(
                        PositionMapper.fromString(moveDTO.fromPos()),
                        PositionMapper.fromString(moveDTO.toPos())),
                    moveDTO.promotionPiece()
                    );

            if (gameStatus == null) {
                // Illegal move. Ignore
            } else if (gameStatus == GameStatus.ONGOING) {
                // Game proceeds
            } else {
                handleGameEnd(game, gameStatus);
            }

        } finally {
            lock.unlock();
        }
    }

    private boolean isUserTurn(Game game, UUID userId){
        if (game.isWhiteTurn()){
            return game.getWhiteId().equals(userId);
        } else {
            return game.getBlackId().equals(userId);
        }
    }

    private Optional<Game> getGame(String gameString){
        UUID gameId;
        try {
            gameId = UUID.fromString(gameString);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        return Optional.ofNullable(games.get(gameId));
    }

    private void handleGameEnd(Game game, GameStatus gameStatus){
        // TODO: Handle game end. Mark game over with winner, return appropriate message. Disconnect WS? Or keep-alive for chat. Calculate ELO. Etc.
    }
}
