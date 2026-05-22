package com.blikeng.chess.controller;

import com.blikeng.chess.dto.GameDTO;
import com.blikeng.chess.dto.GamePreviewDTO;
import com.blikeng.chess.service.GameHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameHistoryController {
    private final GameHistoryService gameHistoryService;

    public GameHistoryController(GameHistoryService gameHistoryService) {
        this.gameHistoryService = gameHistoryService;
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<List<GamePreviewDTO>> getUserGames(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page
    ) {
        return ResponseEntity.ok(gameHistoryService.getGameHistory(username, page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GameDTO> getGame(@PathVariable String id) {
        return ResponseEntity.ok(gameHistoryService.getGame(id));
    }
}