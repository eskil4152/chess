package com.blikeng.chess.controller;

import com.blikeng.chess.dto.GamePreviewDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @GetMapping
    public String getSelf() {
        return "Self";
    }

    @GetMapping("/games")
    public ResponseEntity<List<GamePreviewDTO>> getSelfGames() {
        return ResponseEntity.ok(null);
    }

    @GetMapping("/{username}")
    public String getUser(
            @PathVariable String username
    ) {
        return "User " + username;
    }

    @GetMapping("/{username}/games")
    public ResponseEntity<List<GamePreviewDTO>> getUserGames(
            @PathVariable String username
    ) {
        return ResponseEntity.ok(null);
    }
}
