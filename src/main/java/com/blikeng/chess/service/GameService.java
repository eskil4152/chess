package com.blikeng.chess.service;

import com.blikeng.chess.entity.GameEntity;
import com.blikeng.chess.repository.GameRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.UUID;

@Service
public class GameService {
    private final GameRepository gameRepository;

    public GameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    private final HashMap<UUID, GameEntity> games = new HashMap<>();
}
