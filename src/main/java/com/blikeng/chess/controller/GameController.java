package com.blikeng.chess.controller;

import com.blikeng.chess.dto.GameDTO;
import com.blikeng.chess.dto.GamePreviewDTO;
import com.blikeng.chess.dto.GameStateDTO;
import com.blikeng.chess.service.GameHistoryService;
import com.blikeng.chess.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameController {
    private final GameService gameService;
    private final GameHistoryService gameHistoryService;

    public GameController(GameService gameService, GameHistoryService gameHistoryService) {
        this.gameService = gameService;
        this.gameHistoryService = gameHistoryService;
    }

    @GetMapping("/active")
    public ResponseEntity<GameStateDTO> getGameState(){
        GameStateDTO gameState = gameService.restoreGameState();

        return ResponseEntity.ok(gameState);
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<List<GamePreviewDTO>> getUserGames(
        @PathVariable String username,
        @RequestParam(defaultValue = "0") int page
    ){
        return ResponseEntity.ok(gameHistoryService.getGameHistory(username, page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GameDTO> getGame(
            @PathVariable String id
    ){
        return ResponseEntity.ok(gameHistoryService.getGame(id));
    }
}
