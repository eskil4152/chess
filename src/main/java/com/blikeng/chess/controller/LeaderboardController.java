package com.blikeng.chess.controller;

import com.blikeng.chess.dto.LeaderboardPlayerDTO;
import com.blikeng.chess.service.LeaderboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {
    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/bullet")
    public ResponseEntity<List<LeaderboardPlayerDTO>> getBulletLeaderboard(
        @RequestParam(defaultValue = "0") int page
    ) {
        return ResponseEntity
            .ok()
            .body(leaderboardService.getLeaderboardPlayers("BULLET", page));
    }

    @GetMapping("/blitz")
    public ResponseEntity<List<LeaderboardPlayerDTO>> getBlitzLeaderboard(
        @RequestParam(defaultValue = "0") int page
    ) {
        return ResponseEntity
            .ok()
            .body(leaderboardService.getLeaderboardPlayers("BLITZ", page));
    }

    @GetMapping("/rapid")
    public ResponseEntity<List<LeaderboardPlayerDTO>> getRapidLeaderboard(
        @RequestParam(defaultValue = "0") int page
    ) {
        return ResponseEntity
            .ok()
            .body(leaderboardService.getLeaderboardPlayers("RAPID", page));
    }

    @GetMapping("/classical")
    public ResponseEntity<List<LeaderboardPlayerDTO>> getClassicalLeaderboard(
        @RequestParam(defaultValue = "0") int page
    ) {
        return ResponseEntity
            .ok()
            .body(leaderboardService.getLeaderboardPlayers("CLASSICAL", page));
    }
}