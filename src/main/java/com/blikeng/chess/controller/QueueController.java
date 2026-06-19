package com.blikeng.chess.controller;

import com.blikeng.chess.dto.TimeControlDTO;
import com.blikeng.chess.service.MatchmakingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Matchmaking queue endpoints (base path {@code /api/queue}).
 *
 * <p>{@code POST} joins the queue for a given time control; {@code DELETE} leaves it.
 */
@RestController
@RequestMapping("/api/queue")
public class QueueController {
    private final MatchmakingService matchmakingService;

    public QueueController(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @PostMapping
    public ResponseEntity<String> joinQueue(@RequestBody TimeControlDTO timeControlDTO) {
        matchmakingService.queuePlayer(timeControlDTO);

        return ResponseEntity.ok("Joined queue");
    }

    @DeleteMapping
    public ResponseEntity<String> leaveQueue() {
        matchmakingService.dequeuePlayer();

        return ResponseEntity.ok("Left queue");
    }
}
