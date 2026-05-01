package com.blikeng.chess.controller;

import com.blikeng.chess.dto.GameDTO;
import com.blikeng.chess.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameController {
    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/{username}")
    public ResponseEntity<List<GameDTO>> getUserGames(
            @PathVariable String username
    ){
        return null;
    }
}
