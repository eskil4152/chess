package com.blikeng.chess.controller;

import com.blikeng.chess.dto.GameDTO;
import com.blikeng.chess.dto.GamePreviewDTO;
import com.blikeng.chess.service.GameHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameController {
    private final GameHistoryService gameHistoryService;

    public GameController(GameHistoryService gameHistoryService) {
        this.gameHistoryService = gameHistoryService;
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<List<GamePreviewDTO>> getUserGames(
            @PathVariable String username
    ){
        return ResponseEntity.ok(gameHistoryService.getGameHistory(username));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GameDTO> getGame(
            @PathVariable String id
    ){
        return ResponseEntity.ok(gameHistoryService.getGame(id));
    }
}
