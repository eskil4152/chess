package com.blikeng.chess.unit.service;

import com.blikeng.chess.dto.PasswordDTO;
import com.blikeng.chess.dto.ProfileDTO;
import com.blikeng.chess.dto.ProfileEditDTO;
import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.types.BadEditException;
import com.blikeng.chess.exception.types.InvalidPasswordException;
import com.blikeng.chess.exception.types.InvalidUserException;
import com.blikeng.chess.exception.types.UserNotFoundException;
import com.blikeng.chess.model.GameStatus;
import com.blikeng.chess.model.timecontrol.TimeControl;
import com.blikeng.chess.repository.FriendRepository;
import com.blikeng.chess.repository.UserRepository;
import com.blikeng.chess.security.JwtPrincipal;
import com.blikeng.chess.security.PasswordService;
import com.blikeng.chess.security.UserRole;
import com.blikeng.chess.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock FriendRepository friendRepository;
    @Mock PasswordService passwordService;
    @InjectMocks UserService userService;

    private UserEntity user;

    @BeforeEach
    void setup() {
        user = new UserEntity("someUser", "hash");
        var principal = new JwtPrincipal(user.getId(), user.getUsername(), UserRole.USER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- Get User ---

    @Test
    void getUserShouldThrowWhenPrincipalIsNull() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> userService.getUser("someUser"))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void getUserShouldThrowWhenUserIdIsNull() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new JwtPrincipal(null, "someUser", UserRole.USER), null));
        assertThatThrownBy(() -> userService.getUser("someUser"))
                .isInstanceOf(InvalidUserException.class);
    }

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

    @Test
    void getUserShouldReturnIsFriendTrueWhenFriends() {
        UserEntity other = new UserEntity("otherUser", "hash");
        when(userRepository.findByUsernameIgnoreCase("otherUser")).thenReturn(Optional.of(other));
        when(friendRepository.existsById(any())).thenReturn(true);
        ProfileDTO dto = userService.getUser("otherUser");
        assertThat(dto.isFriend()).isTrue();
    }

    @Test
    void getUserShouldReturnIsFriendFalseForSelf() {
        when(userRepository.findByUsernameIgnoreCase("someUser")).thenReturn(Optional.of(user));
        ProfileDTO dto = userService.getUser("someUser");
        assertThat(dto.isFriend()).isFalse();
        verify(friendRepository, never()).existsById(any());
    }


    // --- Update ELO ---
    @Test
    void updateUserEloShouldThrowWhenUserNotFound() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.updateUserElo(TimeControl.BLITZ_5_0, UUID.randomUUID(), UUID.randomUUID(), GameStatus.WHITE_WIN))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updateUserEloShouldDoNothingWhenOngoing() {
        userService.updateUserElo(TimeControl.BLITZ_5_0, UUID.randomUUID(), UUID.randomUUID(), GameStatus.ONGOING);
        verify(userRepository, never()).findById(any());
    }

    @Test
    void updateUserEloShouldUpdateElosOnWhiteWin() {
        UserEntity white = new UserEntity("white", "h");
        UserEntity black = new UserEntity("black", "h");
        UUID whiteId = white.getId();
        UUID blackId = black.getId();
        white.setBlitzElo(800);
        black.setBlitzElo(800);

        when(userRepository.findById(whiteId)).thenReturn(Optional.of(white));
        when(userRepository.findById(blackId)).thenReturn(Optional.of(black));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.updateUserElo(TimeControl.BLITZ_5_0, whiteId, blackId, GameStatus.WHITE_WIN);

        assertThat(white.getBlitzElo()).isNotEqualTo(800);
        assertThat(black.getBlitzElo()).isNotEqualTo(800);
        assertThat(white.getBlitzElo()).isGreaterThan(black.getBlitzElo());
    }

    @Test
    void updateUserEloShouldUpdateElosOnBlackWin() {
        UserEntity white = new UserEntity("white", "h");
        UserEntity black = new UserEntity("black", "h");
        UUID whiteId = white.getId();
        UUID blackId = black.getId();
        white.setBlitzElo(800);
        black.setBlitzElo(800);

        when(userRepository.findById(blackId)).thenReturn(Optional.of(black));
        when(userRepository.findById(whiteId)).thenReturn(Optional.of(white));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.updateUserElo(TimeControl.BLITZ_5_0, whiteId, blackId, GameStatus.BLACK_WIN);

        assertThat(black.getBlitzElo()).isNotEqualTo(800);
        assertThat(white.getBlitzElo()).isNotEqualTo(800);
        assertThat(black.getBlitzElo()).isGreaterThan(white.getBlitzElo());
    }

    @Test
    void updateUserEloShouldNotChangeElosOnDraw() {
        UserEntity white = new UserEntity("white", "h");
        UserEntity black = new UserEntity("black", "h");
        UUID whiteId = white.getId();
        UUID blackId = black.getId();
        white.setBlitzElo(800);
        black.setBlitzElo(800);

        when(userRepository.findById(any())).thenAnswer(inv -> {
            UUID id = inv.getArgument(0);
            if (id.equals(whiteId)) return Optional.of(white);
            return Optional.of(black);
        });
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.updateUserElo(TimeControl.BLITZ_5_0, whiteId, blackId, GameStatus.DRAW);

        assertThat(white.getBlitzElo()).isEqualTo(black.getBlitzElo());
    }

    // --- been2400 ---
    @Test
    void updateUserEloShouldSetBeen2400WhenWhiteCrosses2400() {
        UserEntity white = new UserEntity("white", "h");
        UserEntity black = new UserEntity("black", "h");
        white.setBlitzElo(2399);
        black.setBlitzElo(2399);

        when(userRepository.findById(white.getId())).thenReturn(Optional.of(white));
        when(userRepository.findById(black.getId())).thenReturn(Optional.of(black));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.updateUserElo(TimeControl.BLITZ_5_0, white.getId(), black.getId(), GameStatus.WHITE_WIN);

        assertThat(white.isBeen2400Blitz()).isTrue();
        assertThat(black.isBeen2400Blitz()).isFalse();
    }

    @Test
    void updateUserEloShouldSetBeen2400WhenBlackCrosses2400() {
        UserEntity white = new UserEntity("white", "h");
        UserEntity black = new UserEntity("black", "h");
        white.setBlitzElo(2399);
        black.setBlitzElo(2399);

        when(userRepository.findById(white.getId())).thenReturn(Optional.of(white));
        when(userRepository.findById(black.getId())).thenReturn(Optional.of(black));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.updateUserElo(TimeControl.BLITZ_5_0, white.getId(), black.getId(), GameStatus.BLACK_WIN);

        assertThat(black.isBeen2400Blitz()).isTrue();
        assertThat(white.isBeen2400Blitz()).isFalse();
    }

    // --- Update User ---

    @Test
    void updateUserShouldUpdateBio() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        userService.updateUser(new ProfileEditDTO("bio", "new bio"));
        assertThat(user.getBio()).isEqualTo("new bio");
        verify(userRepository).save(user);
    }

    @Test
    void updateUserShouldUpdateAvatarUrl() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        userService.updateUser(new ProfileEditDTO("avatarUrl", "https://example.com/img.png"));
        assertThat(user.getAvatarUrl()).isEqualTo("https://example.com/img.png");
        verify(userRepository).save(user);
    }

    @Test
    void updateUserShouldTrimValue() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        userService.updateUser(new ProfileEditDTO("bio", "  trimmed  "));
        assertThat(user.getBio()).isEqualTo("trimmed");
    }

    @Test
    void updateUserShouldThrowOnUnknownField() {
        ProfileEditDTO profileEditDTO = new ProfileEditDTO("username", "hacker");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> userService.updateUser(profileEditDTO))
                .isInstanceOf(BadEditException.class);
    }

    @Test
    void updateUserShouldThrowWhenPrincipalIsNull() {
        ProfileEditDTO profileEditDTO = new ProfileEditDTO("bio", "x");

        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> userService.updateUser(profileEditDTO))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void updateUserShouldThrowWhenUserNotFound() {
        ProfileEditDTO profileEditDTO = new ProfileEditDTO("bio", "x");

        when(userRepository.findById(user.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.updateUser(profileEditDTO))
                .isInstanceOf(InvalidUserException.class);
    }

    // --- Update Password ---

    @Test
    void updatePasswordShouldHashAndSaveNewPassword() {
        PasswordDTO passwordDTO = new PasswordDTO("oldPass", "newPass123");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordService.checkPassword("oldPass", user.getPassword())).thenReturn(true);
        when(passwordService.hashPassword("newPass123")).thenReturn("hashed");

        userService.updatePassword(passwordDTO);

        assertThat(user.getPassword()).isEqualTo("hashed");
        verify(userRepository).save(user);
    }

    @Test
    void updatePasswordShouldThrowOnWrongOldPassword() {
        PasswordDTO passwordDTO = new PasswordDTO("wrong", "newPass123");

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordService.checkPassword("wrong", user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> userService.updatePassword(passwordDTO))
                .isInstanceOf(InvalidPasswordException.class);
    }

    @Test
    void updatePasswordShouldThrowWhenNewPasswordTooShort() {
        PasswordDTO passwordDTO = new PasswordDTO("old", "short");

        assertThatThrownBy(() -> userService.updatePassword(passwordDTO))
                .isInstanceOf(BadEditException.class);
    }

    @Test
    void updatePasswordShouldThrowWhenNewPasswordTooLong() {
        String tooLong = "a".repeat(129);
        PasswordDTO passwordDTO = new PasswordDTO("old", tooLong);

        assertThatThrownBy(() -> userService.updatePassword(passwordDTO))
                .isInstanceOf(BadEditException.class);
    }

    @Test
    void updatePasswordShouldThrowWhenNewPasswordBlank() {
        PasswordDTO passwordDTO = new PasswordDTO("old", "   ");

        assertThatThrownBy(() -> userService.updatePassword(passwordDTO))
                .isInstanceOf(BadEditException.class);
    }

    @Test
    void updatePasswordShouldThrowWhenPrincipalIsNull() {
        PasswordDTO passwordDTO = new PasswordDTO("old", "newPass123");

        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> userService.updatePassword(passwordDTO))
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void updatePasswordShouldThrowWhenUserNotFound() {
        PasswordDTO passwordDTO = new PasswordDTO("old", "newPass123");

        when(userRepository.findById(user.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.updatePassword(passwordDTO))
                .isInstanceOf(InvalidUserException.class);
    }

    // --- KFactor ---
    @Test
    void updateUserEloShouldUseKFactor10WhenBeen2400() {
        // equal ELO → expected=0.5, K=10 win: delta = round(10*0.5) = 5
        UserEntity white = new UserEntity("white", "h");
        UserEntity black = new UserEntity("black", "h");
        white.setBlitzElo(1000);
        black.setBlitzElo(1000);
        white.setBeen2400Blitz(true);

        when(userRepository.findById(white.getId())).thenReturn(Optional.of(white));
        when(userRepository.findById(black.getId())).thenReturn(Optional.of(black));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.updateUserElo(TimeControl.BLITZ_5_0, white.getId(), black.getId(), GameStatus.WHITE_WIN);

        assertThat(white.getBlitzElo()).isEqualTo(1005);
    }

    @Test
    void updateUserEloShouldUseKFactor20WhenOver30Games() {
        // equal ELO → expected=0.5, K=20 win: delta = round(20*0.5) = 10
        UserEntity white = new UserEntity("white", "h");
        UserEntity black = new UserEntity("black", "h");
        white.setBlitzElo(1000);
        black.setBlitzElo(1000);
        white.setBlitzGames(30);

        when(userRepository.findById(white.getId())).thenReturn(Optional.of(white));
        when(userRepository.findById(black.getId())).thenReturn(Optional.of(black));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.updateUserElo(TimeControl.BLITZ_5_0, white.getId(), black.getId(), GameStatus.WHITE_WIN);

        assertThat(white.getBlitzElo()).isEqualTo(1010);
    }
}