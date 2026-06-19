package com.blikeng.chess.service.game;

import com.blikeng.chess.dto.GameStateDTO;
import com.blikeng.chess.exception.types.GameNotFoundException;
import com.blikeng.chess.exception.types.InvalidUserException;
import com.blikeng.chess.model.Game;
import com.blikeng.chess.security.JwtPrincipal;
import com.blikeng.chess.security.JwtService;
import org.springframework.stereotype.Service;

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

    public GameViewService(ActiveGameStore activeGameStore) {
        this.activeGameStore = activeGameStore;
    }

    public GameStateDTO restoreGameState() {
        return restoreGameState(null);
    }

    public GameStateDTO restoreGameState(String gameIdString) {
        JwtPrincipal jwtPrincipal = JwtService.getCurrentUser();
        if (jwtPrincipal == null) throw new InvalidUserException();

        UUID userId = jwtPrincipal.userId();

        if (gameIdString == null){
            return activeGameStore.findByUser(userId)
                .map(this::buildGameStateDTO)
                .orElseThrow(GameNotFoundException::new);
        } else {
            Game game = activeGameStore.get(gameIdString).orElseThrow(GameNotFoundException::new);

            if (!game.getWhiteId().equals(userId) && !game.getBlackId().equals(userId)) {
                game.getSpectators().add(userId);
            }

            return buildGameStateDTO(game);
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
