package com.blikeng.chess.controller;

import com.blikeng.chess.dto.GameStateDTO;
import com.blikeng.chess.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for retrieving active games.
 *
 * <p>{@code GET /{id}} retrieves a specific game if active. {@code GET} only works if the user has an active game.
 */
@RestController
@RequestMapping("/api/games/active")
public class ActiveGameController {
    private final GameService gameService;

    public ActiveGameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping
    public ResponseEntity<GameStateDTO> getActiveGame() {
        return ResponseEntity.ok(gameService.restoreGameState());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GameStateDTO> spectateGame(@PathVariable String id) {
        return ResponseEntity.ok(gameService.restoreGameState(id));
    }
}