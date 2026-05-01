package com.blikeng.chess.service;

import com.blikeng.chess.dto.GameDTO;
import com.blikeng.chess.dto.GamePreviewDTO;
import com.blikeng.chess.exception.ErrorTypes.GameNotFoundException;
import com.blikeng.chess.exception.ErrorTypes.InvalidUUIDException;
import com.blikeng.chess.exception.ErrorTypes.UserNotFoundException;
import com.blikeng.chess.repository.GameRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class GameHistoryService {
    private final GameRepository gameRepository;

    public GameHistoryService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    public GameDTO getGame(String gameId){
        UUID gameUUID;

        try {
            gameUUID = UUID.fromString(gameId);
        } catch (IllegalArgumentException e) {
            throw new InvalidUUIDException();
        }

        return gameRepository.findById(gameUUID)
                .map(game -> new GameDTO(
                        game.getId(),
                        game.getBlack().getUsername(),
                        game.getWhite().getUsername(),
                        game.getStatus(),
                        ""
                        // TODO: implement moves
                ))
                .orElseThrow(GameNotFoundException::new);
    }

    public List<GamePreviewDTO> getGameHistory(String username) {
        if (username == null || username.trim().isBlank()) throw new UserNotFoundException();
        username = username.trim();

        return gameRepository.findAllByUsername(username)
                .stream()
                .map(game -> new GamePreviewDTO(
                        game.getId(),
                        game.getBlack().getUsername(),
                        game.getWhite().getUsername(),
                        game.getStatus()
                ))
                .toList();
    }
}
