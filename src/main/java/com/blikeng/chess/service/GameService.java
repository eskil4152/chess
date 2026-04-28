package com.blikeng.chess.service;

import com.blikeng.chess.entity.GameEntity;
import com.blikeng.chess.repository.GameRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.UUID;

@Service
public class GameService {
    private GameRepository gameRepository;

    private final HashMap<UUID, GameEntity> games = new HashMap<>();
}
