package com.blikeng.chess.unit.service;

import com.blikeng.chess.dto.GameDTO;
import com.blikeng.chess.dto.GamePreviewDTO;
import com.blikeng.chess.entity.GameEntity;
import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.types.GameNotFoundException;
import com.blikeng.chess.exception.types.InvalidUUIDException;
import com.blikeng.chess.exception.types.UserNotFoundException;
import com.blikeng.chess.model.GameStatus;
import com.blikeng.chess.repository.GameRepository;
import com.blikeng.chess.service.GameHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameHistoryServiceTest {

    @Mock GameRepository gameRepository;
    @InjectMocks GameHistoryService gameHistoryService;

    private GameEntity gameEntity;
    private UserEntity white;
    private UserEntity black;

    @BeforeEach
    void setup() {
        white = new UserEntity("white", "h");
        black = new UserEntity("black", "h");
        gameEntity = new GameEntity(white, black, GameStatus.WHITE_WIN, Instant.now());
    }

    // --- Get game ---
    @Test
    void getGameShouldReturnDTOForValidId() {
        UUID id = gameEntity.getId();
        when(gameRepository.findById(id)).thenReturn(Optional.of(gameEntity));

        GameDTO dto = gameHistoryService.getGame(id.toString());

        assertThat(dto.gameId()).isEqualTo(id);
        assertThat(dto.whiteUsername()).isEqualTo("white");
        assertThat(dto.blackUsername()).isEqualTo("black");
        assertThat(dto.status()).isEqualTo(GameStatus.WHITE_WIN);
    }

    @Test
    void getGameShouldThrowOnInvalidUUID() {
        assertThatThrownBy(() -> gameHistoryService.getGame("not-a-uuid"))
                .isInstanceOf(InvalidUUIDException.class);
    }

    @Test
    void getGameShouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(gameRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> gameHistoryService.getGame(id.toString()))
                .isInstanceOf(GameNotFoundException.class);
    }


    // --- Get Game History ---
    @Test
    void getGameHistoryShouldReturnListOfPreviews() {
        when(gameRepository.findAllByUsername("white")).thenReturn(List.of(gameEntity));

        List<GamePreviewDTO> history = gameHistoryService.getGameHistory("white");

        assertThat(history).hasSize(1);
        assertThat(history.get(0).whiteUsername()).isEqualTo("white");
        assertThat(history.get(0).status()).isEqualTo(GameStatus.WHITE_WIN);
    }

    @Test
    void getGameHistoryShouldThrowOnNullUsername() {
        assertThatThrownBy(() -> gameHistoryService.getGameHistory(null))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getGameHistoryShouldThrowOnBlankUsername() {
        assertThatThrownBy(() -> gameHistoryService.getGameHistory("  "))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getGameHistoryShouldTrimUsername() {
        when(gameRepository.findAllByUsername("white")).thenReturn(List.of());
        List<GamePreviewDTO> result = gameHistoryService.getGameHistory("  white  ");
        assertThat(result).isEmpty();
    }
}