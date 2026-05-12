package com.blikeng.chess.unit.service;

import com.blikeng.chess.dto.ProfileDTO;
import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.types.InvalidUserException;
import com.blikeng.chess.exception.types.UserNotFoundException;
import com.blikeng.chess.model.GameStatus;
import com.blikeng.chess.repository.UserRepository;
import com.blikeng.chess.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @InjectMocks UserService userService;

    private UserEntity user;

    @BeforeEach
    void setup() {
        user = new UserEntity("someUser", "hash");
    }

    // --- Get User ---
    @Test
    void getUserShouldReturnProfileDTOWhenFound() {
        when(userRepository.findByUsernameIgnoreCase("someUser")).thenReturn(Optional.of(user));
        ProfileDTO dto = userService.getUser("someUser");
        assertThat(dto.username()).isEqualTo("someUser");
    }

    @Test
    void getUserShouldThrowOnNullUsername() {
        assertThatThrownBy(() -> userService.getUser(null))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getUserShouldThrowOnBlankUsername() {
        assertThatThrownBy(() -> userService.getUser("   "))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getUserShouldThrowWhenNotFound() {
        when(userRepository.findByUsernameIgnoreCase("unknown")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getUser("unknown"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getUserShouldTrimWhitespace() {
        when(userRepository.findByUsernameIgnoreCase("someUser")).thenReturn(Optional.of(user));
        ProfileDTO dto = userService.getUser("  someUser  ");
        assertThat(dto.username()).isEqualTo("someUser");
    }


    // --- Update ELO ---
    @Test
    void updateUserEloShouldThrowWhenUserNotFound() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.updateUserElo(UUID.randomUUID(), UUID.randomUUID(), GameStatus.WHITE_WIN))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updateUserEloShouldDoNothingWhenOngoing() {
        userService.updateUserElo(UUID.randomUUID(), UUID.randomUUID(), GameStatus.ONGOING);
        verify(userRepository, never()).findById(any());
    }

    @Test
    void updateUserEloShouldUpdateElosOnWhiteWin() {
        UserEntity white = new UserEntity("white", "h");
        UserEntity black = new UserEntity("black", "h");
        UUID whiteId = white.getId();
        UUID blackId = black.getId();
        white.setElo(800);
        black.setElo(800);

        when(userRepository.findById(whiteId)).thenReturn(Optional.of(white));
        when(userRepository.findById(blackId)).thenReturn(Optional.of(black));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.updateUserElo(whiteId, blackId, GameStatus.WHITE_WIN);

        assertThat(white.getElo()).isNotEqualTo(800);
        assertThat(black.getElo()).isNotEqualTo(800);
        assertThat(white.getElo()).isGreaterThan(black.getElo());
    }

    @Test
    void updateUserEloShouldUpdateElosOnBlackWin() {
        UserEntity white = new UserEntity("white", "h");
        UserEntity black = new UserEntity("black", "h");
        UUID whiteId = white.getId();
        UUID blackId = black.getId();
        white.setElo(800);
        black.setElo(800);

        when(userRepository.findById(blackId)).thenReturn(Optional.of(black));
        when(userRepository.findById(whiteId)).thenReturn(Optional.of(white));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.updateUserElo(whiteId, blackId, GameStatus.BLACK_WIN);

        assertThat(black.getElo()).isNotEqualTo(800);
        assertThat(white.getElo()).isNotEqualTo(800);
        assertThat(black.getElo()).isGreaterThan(white.getElo());
    }

    @Test
    void updateUserEloShouldNotChangeElosOnDraw() {
        UserEntity white = new UserEntity("white", "h");
        UserEntity black = new UserEntity("black", "h");
        UUID whiteId = white.getId();
        UUID blackId = black.getId();
        white.setElo(800);
        black.setElo(800);

        when(userRepository.findById(any())).thenAnswer(inv -> {
            UUID id = inv.getArgument(0);
            if (id.equals(whiteId)) return Optional.of(white);
            return Optional.of(black);
        });
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.updateUserElo(whiteId, blackId, GameStatus.DRAW);

        assertThat(white.getElo()).isEqualTo(black.getElo());
    }

    // --- been2400 ---
    @Test
    void updateUserEloShouldSetBeen2400WhenWhiteCrosses2400() {
        UserEntity white = new UserEntity("white", "h");
        UserEntity black = new UserEntity("black", "h");
        white.setElo(2399);
        black.setElo(2399);

        when(userRepository.findById(white.getId())).thenReturn(Optional.of(white));
        when(userRepository.findById(black.getId())).thenReturn(Optional.of(black));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.updateUserElo(white.getId(), black.getId(), GameStatus.WHITE_WIN);

        assertThat(white.isBeen2400()).isTrue();
        assertThat(black.isBeen2400()).isFalse();
    }

    @Test
    void updateUserEloShouldSetBeen2400WhenBlackCrosses2400() {
        UserEntity white = new UserEntity("white", "h");
        UserEntity black = new UserEntity("black", "h");
        white.setElo(2399);
        black.setElo(2399);

        when(userRepository.findById(white.getId())).thenReturn(Optional.of(white));
        when(userRepository.findById(black.getId())).thenReturn(Optional.of(black));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.updateUserElo(white.getId(), black.getId(), GameStatus.BLACK_WIN);

        assertThat(black.isBeen2400()).isTrue();
        assertThat(white.isBeen2400()).isFalse();
    }

    // --- KFactor ---
    @Test
    void updateUserEloShouldUseKFactor10WhenBeen2400() {
        // equal ELO → expected=0.5, K=10 win: delta = round(10*0.5) = 5
        UserEntity white = new UserEntity("white", "h");
        UserEntity black = new UserEntity("black", "h");
        white.setElo(1000);
        black.setElo(1000);
        white.setBeen2400(true);

        when(userRepository.findById(white.getId())).thenReturn(Optional.of(white));
        when(userRepository.findById(black.getId())).thenReturn(Optional.of(black));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.updateUserElo(white.getId(), black.getId(), GameStatus.WHITE_WIN);

        assertThat(white.getElo()).isEqualTo(1005);
    }

    @Test
    void updateUserEloShouldUseKFactor20WhenOver30Games() {
        // equal ELO → expected=0.5, K=20 win: delta = round(20*0.5) = 10
        UserEntity white = new UserEntity("white", "h");
        UserEntity black = new UserEntity("black", "h");
        white.setElo(1000);
        black.setElo(1000);
        white.setGames(30);

        when(userRepository.findById(white.getId())).thenReturn(Optional.of(white));
        when(userRepository.findById(black.getId())).thenReturn(Optional.of(black));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.updateUserElo(white.getId(), black.getId(), GameStatus.WHITE_WIN);

        assertThat(white.getElo()).isEqualTo(1010);
    }
}