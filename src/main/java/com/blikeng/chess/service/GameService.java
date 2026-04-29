package com.blikeng.chess.service;

import com.blikeng.chess.dto.GameStartedDTO;
import com.blikeng.chess.dto.MoveDTO;
import com.blikeng.chess.engine.MoveExecutor;
import com.blikeng.chess.engine.PositionMapper;
import com.blikeng.chess.entity.GameEntity;
import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.model.Game;
import com.blikeng.chess.exception.ErrorTypes.GameNotFoundException;
import com.blikeng.chess.model.GameStatus;
import com.blikeng.chess.model.Move;
import com.blikeng.chess.model.Position;
import com.blikeng.chess.repository.GameRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class GameService {
    private final AuthService authService;
    private final GameRepository gameRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MoveExecutor moveExecutor = new MoveExecutor();

    public GameService(
            AuthService authService,
            GameRepository gameRepository
    ) {
        this.authService = authService;
        this.gameRepository = gameRepository;
    }

    private final ConcurrentHashMap<UUID, Game> games = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, UserEntity> queue = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

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

    public void removeSession(UUID userId, WebSocketSession session) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null) return;
        sessions.remove(session);
        if (sessions.isEmpty()) {
            userSessions.remove(userId);
            queue.remove(userId);
        }
    }

    public void beginGame(UserEntity whitePlayer, UserEntity blackPlayer) {
        Game game = new Game(whitePlayer.getId(), whitePlayer.getUsername(), blackPlayer.getId(), blackPlayer.getUsername());
        game.setWhiteKingPosition(new Position(7, 4));
        game.setBlackKingPosition(new Position(0, 4));

        gameRepository.save(new GameEntity(
                game.getId(),
                whitePlayer,
                blackPlayer,
                GameStatus.ONGOING,
                Instant.now()
        ));

        games.put(game.getId(), game);

        try {
            String payload = objectMapper.writeValueAsString(new GameStartedDTO(
                    game.getId(),
                    whitePlayer.getId(),
                    whitePlayer.getUsername(),
                    blackPlayer.getId(),
                    blackPlayer.getUsername()
            ));
            sendToUser(whitePlayer.getId(), payload);
            sendToUser(blackPlayer.getId(), payload);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void makeMove(UUID userId, MoveDTO moveDTO) {
        Game game = getGame(moveDTO.gameId()).orElseThrow(GameNotFoundException::new);

        ReentrantLock lock = game.lockGame();
        lock.lock();

        try {
            if (!isUserTurn(game, userId)) return;

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

    public void saveSession(UUID userId, WebSocketSession session) {
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);

        games.values().stream()
                .filter(g -> g.getWhiteId().equals(userId) || g.getBlackId().equals(userId))
                .findFirst()
                .ifPresent(game -> {
                    try {
                        String payload = objectMapper.writeValueAsString(new GameStartedDTO(
                                game.getId(),
                                game.getWhiteId(),
                                game.getWhiteUsername(),
                                game.getBlackId(),
                                game.getBlackUsername()
                        ));
                        session.sendMessage(new TextMessage(payload));
                    } catch (IOException e) {
                        // session will need to retry
                    }
                });
    }

    private void sendToUser(UUID userId, String payload) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null) return;
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) session.sendMessage(new TextMessage(payload));
            } catch (IOException e) {
                // session will need to retry
            }
        }
    }

    private boolean isUserTurn(Game game, UUID userId) {
        return game.isWhiteTurn() ? game.getWhiteId().equals(userId) : game.getBlackId().equals(userId);
    }

    private Optional<Game> getGame(String gameString) {
        try {
            return Optional.ofNullable(games.get(UUID.fromString(gameString)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private void handleGameEnd(Game game, GameStatus gameStatus) {
        // TODO: Handle game end. Mark game over with winner, return appropriate message. Disconnect WS? Or keep-alive for chat. Calculate ELO. Etc.
    }
}
