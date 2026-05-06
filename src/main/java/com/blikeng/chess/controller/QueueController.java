package com.blikeng.chess.controller;

import com.blikeng.chess.service.MatchmakingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/queue")
public class QueueController {
    private final MatchmakingService matchmakingService;

    public QueueController(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @PostMapping
    public ResponseEntity<String> joinQueue() {
        matchmakingService.queuePlayer();

        return ResponseEntity.ok("Joined queue");
    }

    @DeleteMapping
    public ResponseEntity<String> leaveQueue() {
        matchmakingService.dequeuePlayer();

        return ResponseEntity.ok("Left queue");
    }
}
