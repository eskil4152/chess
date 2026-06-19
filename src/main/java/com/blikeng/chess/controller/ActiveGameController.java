package com.blikeng.chess.controller;

import com.blikeng.chess.dto.GameStateDTO;
import com.blikeng.chess.service.game.GameViewService;
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
    private final GameViewService gameViewService;

    public ActiveGameController(GameViewService gameViewService) {
        this.gameViewService = gameViewService;
    }

    @GetMapping
    public ResponseEntity<GameStateDTO> getActiveGame() {
        return ResponseEntity.ok(gameViewService.restoreGameState());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GameStateDTO> spectateGame(@PathVariable String id) {
        return ResponseEntity.ok(gameViewService.restoreGameState(id));
    }
}