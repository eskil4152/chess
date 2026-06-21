package com.blikeng.chess.service.game;

import com.blikeng.chess.dto.GameStateDTO;
import com.blikeng.chess.exception.types.GameNotFoundException;
import com.blikeng.chess.exception.types.InvalidUserException;
import com.blikeng.chess.model.Game;
import com.blikeng.chess.security.JwtPrincipal;
import com.blikeng.chess.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Read model for active games: builds the {@link GameStateDTO} a client needs to render a
 * game, computing each side's remaining time from the current turn's elapsed clock.
 *
 * <p>{@link #restoreGameState()} returns the caller's own game; the {@code String} overload
 * returns a game by id and registers a non-participant caller as a spectator.
 */
@Service
public class GameViewService {
    private final ActiveGameStore activeGameStore;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GameViewService(ActiveGameStore activeGameStore, RedisTemplate<String, String> redisTemplate) {
        this.activeGameStore = activeGameStore;
        this.redisTemplate = redisTemplate;
    }

    public GameStateDTO restoreGameState() {
        return restoreGameState(null);
    }

    public GameStateDTO restoreGameState(String gameIdString) {
        JwtPrincipal jwtPrincipal = JwtService.getCurrentUser();
        if (jwtPrincipal == null) throw new InvalidUserException();

        UUID userId = jwtPrincipal.userId();

        if (gameIdString == null){
            Optional<Game> game = activeGameStore.findByUser(userId);
            if (game.isPresent()) return buildGameStateDTO(game.get());

            String gameId = redisTemplate.opsForValue().get("game:user:" + userId);
            if (gameId == null) throw new GameNotFoundException();

            var command = Map.of(
                "action", "RESTORE",
                "userId", userId.toString(),
                "gameId", gameId
            );

            try {
                redisTemplate.convertAndSend("game:" + gameId, objectMapper.writeValueAsString(command));
            } catch (Exception e) {
                // TODO: Custom exception and logger
                e.printStackTrace();
            }
        } else {
            Optional<Game> game = activeGameStore.get(gameIdString);
            if (game.isPresent()) {
                if (!game.get().getWhiteId().equals(userId) && !game.get().getBlackId().equals(userId)) {
                    game.get().getSpectators().add(userId);
                }
                return buildGameStateDTO(game.get());
            }

            var command = Map.of(
                "action", "RESTORE",
                "userId", userId.toString(),
                "gameId", gameIdString
            );

            try {
                redisTemplate.convertAndSend("game:" + gameIdString, objectMapper.writeValueAsString(command));
            } catch (Exception e) {
                // TODO: Custom exception and logger
                e.printStackTrace();
            }
        }

        return null;
    }

    public void pushGameState(String gameId, UUID userId) {
        Game game = activeGameStore.get(gameId).orElseThrow(GameNotFoundException::new);
        GameStateDTO dto = buildGameStateDTO(game);
        try {
            redisTemplate.convertAndSend("user:" + userId, objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            // TODO: Custom exception and logger
            e.printStackTrace();
        }
    }

    private GameStateDTO buildGameStateDTO(Game game) {
        long elapsed = System.currentTimeMillis() - game.getTurnStartTime();
        int whiteRemaining = game.isWhiteTurn()
            ? Math.max(0, game.getWhiteRemainingMs() - (int) elapsed)
            : game.getWhiteRemainingMs();
        int blackRemaining = game.isWhiteTurn()
            ? game.getBlackRemainingMs()
            : Math.max(0, game.getBlackRemainingMs() - (int) elapsed);

        return new GameStateDTO(
            game.getId(),
            game.getWhiteId(),
            game.getWhiteUsername(),
            game.getBlackId(),
            game.getBlackUsername(),
            game.getMoves(),
            game.isWhiteDraw(),
            game.isBlackDraw(),
            game.getWhiteElo(),
            game.getBlackElo(),
            whiteRemaining,
            blackRemaining
        );
    }
}
