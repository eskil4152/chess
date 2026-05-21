package com.blikeng.chess.service;

import com.blikeng.chess.dto.GameDTO;
import com.blikeng.chess.dto.GamePreviewDTO;
import com.blikeng.chess.exception.types.GameNotFoundException;
import com.blikeng.chess.exception.types.InvalidParameterException;
import com.blikeng.chess.exception.types.InvalidUUIDException;
import com.blikeng.chess.exception.types.UserNotFoundException;
import com.blikeng.chess.repository.GameRepository;
import org.springframework.data.domain.PageRequest;
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
        } catch (IllegalArgumentException _) {
            throw new InvalidUUIDException();
        }

        return gameRepository.findById(gameUUID)
                .map(game -> new GameDTO(
                        game.getId(),
                        game.getBlack().getUsername(),
                        game.getWhite().getUsername(),
                        game.getStatus(),
                        game.getMoves()
                ))
                .orElseThrow(GameNotFoundException::new);
    }

    public List<GamePreviewDTO> getGameHistory(String username, int page) {
        if (page < 0) throw new InvalidParameterException();

        if (username == null || username.trim().isBlank()) throw new UserNotFoundException();
        username = username.trim();

        return gameRepository.findByUsernameOrderedByTimestampDesc(username, PageRequest.of(page, 25))
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
