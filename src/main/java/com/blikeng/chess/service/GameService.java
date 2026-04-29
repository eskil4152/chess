package com.blikeng.chess.service;

import com.blikeng.chess.dto.MoveDTO;
import com.blikeng.chess.engine.MoveExecutor;
import com.blikeng.chess.engine.PositionMapper;
import com.blikeng.chess.entity.GameEntity;
import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.model.Game;
import com.blikeng.chess.exception.ErrorTypes.GameNotFoundException;
import com.blikeng.chess.exception.ErrorTypes.InvalidUserException;
import com.blikeng.chess.model.GameStatus;
import com.blikeng.chess.model.Move;
import com.blikeng.chess.model.Position;
import com.blikeng.chess.repository.GameRepository;
import com.blikeng.chess.security.JwtPrincipal;
import com.blikeng.chess.security.JwtService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class GameService {
    private final JwtService jwtService;
    private final AuthService authService;
    private final GameRepository gameRepository;
    private final MoveExecutor moveExecutor = new MoveExecutor();

    public GameService(
            JwtService jwtService,
            AuthService authService,
            GameRepository gameRepository
    ) {
        this.jwtService = jwtService;
        this.authService = authService;
        this.gameRepository = gameRepository;
    }

    private final ConcurrentHashMap<UUID, Game> games = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, UserEntity> queue = new ConcurrentHashMap<>();

    public void queuePlayer(UUID userId) {
        if (queue.containsKey(userId)) return;

        UserEntity user = authService.findUserById(userId).orElseThrow();

        UserEntity matched;

        synchronized (queue) {
            var best = queue.entrySet().stream()
                    .min(Comparator.comparingInt(e -> Math.abs(e.getValue().getElo() - user.getElo())))
                    .filter(e -> Math.abs(e.getValue().getElo() - user.getElo()) <= 200)
                    .orElse(null);

            if (best == null) {
                queue.put(userId, user);
                return;
            }

            queue.remove(best.getKey());
            matched = best.getValue();
        }

        beginGame(matched, user);
    }

    public void dequeuePlayer(UUID userId) {
        queue.remove(userId);
    }

    public void beginGame(UserEntity whitePlayer, UserEntity blackPlayer) {
        Game game = new Game(whitePlayer.getId(), whitePlayer.getUsername(), blackPlayer.getId(), blackPlayer.getUsername());
        game.setWhiteKingPosition(new Position(7, 4));
        game.setBlackKingPosition(new Position(0, 4));

        games.put(game.getId(), game);

        GameEntity gameEntity = new GameEntity(
                game.getId(),
                whitePlayer,
                blackPlayer,
                GameStatus.ONGOING,
                Instant.now()
        );

        gameRepository.save(gameEntity);
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
