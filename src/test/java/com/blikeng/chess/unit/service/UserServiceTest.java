package com.blikeng.chess.unit.service;

import com.blikeng.chess.dto.PasswordDTO;
import com.blikeng.chess.dto.PlayerStatsDTO;
import com.blikeng.chess.dto.ProfileDTO;
import com.blikeng.chess.dto.GameStatRow;
import com.blikeng.chess.dto.ProfileEditDTO;
import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.types.BadEditException;
import com.blikeng.chess.exception.types.InvalidPasswordException;
import com.blikeng.chess.exception.types.InvalidUserException;
import com.blikeng.chess.exception.types.UserNotFoundException;
import com.blikeng.chess.model.EndedBy;
import com.blikeng.chess.model.Game;
import com.blikeng.chess.model.GameStatus;
import com.blikeng.chess.repository.FriendRepository;
import com.blikeng.chess.repository.UserRepository;
import com.blikeng.chess.security.JwtPrincipal;
import com.blikeng.chess.security.PasswordService;
import com.blikeng.chess.security.UserRole;
import com.blikeng.chess.service.game.GameHistoryService;
import com.blikeng.chess.service.game.ActiveGameStore;
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

import java.util.List;
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
    @Mock ActiveGameStore activeGameStore;
    @Mock GameHistoryService gameHistoryService;
    @InjectMocks UserService userService;

    private UserEntity user;
    private UserEntity opponent;

    private GameStatRow game(UserEntity white, UserEntity black, GameStatus status, EndedBy endedBy) {
        return new GameStatRow(white.getId(), status, endedBy);
    }

    private void stubStats(int games, int wins, int losses) {
        user.setRapidGames(games);
        user.setRapidWins(wins);
        user.setRapidLosses(losses);
        user.setRapidElo(1200);
        when(userRepository.findByUsernameIgnoreCase("someUser")).thenReturn(Optional.of(user));
        when(gameHistoryService.getFinishedGameStats(user.getId(),"RAPID")).thenReturn(List.of());
    }

    @BeforeEach
    void setup() {
        user = new UserEntity("someUser", "hash");
        opponent = new UserEntity("opponent", "hash");
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
        when(userRepository.findByUsernameIgnoreCase("opponent")).thenReturn(Optional.of(opponent));
        when(friendRepository.existsById(any())).thenReturn(true);
        ProfileDTO dto = userService.getUser("opponent");
        assertThat(dto.isFriend()).isTrue();
    }

    @Test
    void getUserShouldReturnIsFriendFalseForSelf() {
        when(userRepository.findByUsernameIgnoreCase("someUser")).thenReturn(Optional.of(user));
        ProfileDTO dto = userService.getUser("someUser");
        assertThat(dto.isFriend()).isFalse();
        verify(friendRepository, never()).existsById(any());
    }

    @Test
    void getUserShouldIncludeActiveGameId() {
        UUID gameId = UUID.randomUUID();
        Game game = mock(Game.class);
        when(game.getId()).thenReturn(gameId);
        when(userRepository.findByUsernameIgnoreCase("someUser")).thenReturn(Optional.of(user));
        when(activeGameStore.findByUser(user.getId())).thenReturn(Optional.of(game));

        ProfileDTO dto = userService.getUser("someUser");

        assertThat(dto.activeGameId()).isEqualTo(gameId.toString());
    }

    @Test
    void getUserShouldReturnNullActiveGameIdWhenNotInGame() {
        when(userRepository.findByUsernameIgnoreCase("someUser")).thenReturn(Optional.of(user));
        when(activeGameStore.findByUser(user.getId())).thenReturn(Optional.empty());

        ProfileDTO dto = userService.getUser("someUser");

        assertThat(dto.activeGameId()).isNull();
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

    // --- Player stats ---
    @Test
    void getPlayerStatsShouldThrowWhenUserNotFound() {
        when(userRepository.findByUsernameIgnoreCase("unknown")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getPlayerStats("unknown", "RAPID"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getPlayerStatsShouldBeCaseInsensitiveForTimeControl() {
        user.setRapidElo(1200);
        when(userRepository.findByUsernameIgnoreCase("someUser")).thenReturn(Optional.of(user));
        when(gameHistoryService.getFinishedGameStats(user.getId(),"rapid")).thenReturn(List.of());
        assertThat(userService.getPlayerStats("someUser", "rapid").elo()).isEqualTo(1200);
    }

    @Test
    void getPlayerStatsShouldReturnEloFromUser() {
        stubStats(0, 0, 0);
        assertThat(userService.getPlayerStats("someUser", "RAPID").elo()).isEqualTo(1200);
    }

    @Test
    void getPlayerStatsShouldReturnAggregateCountsFromUser() {
        stubStats(10, 6, 3);
        PlayerStatsDTO dto = userService.getPlayerStats("someUser", "RAPID");
        assertThat(dto.gamesPlayed()).isEqualTo(10);
        assertThat(dto.gamesWon()).isEqualTo(6);
        assertThat(dto.gamesLost()).isEqualTo(3);
        assertThat(dto.gamesDrawn()).isEqualTo(1);
    }

    @Test
    void getPlayerStatsShouldCountWinsByTerminationType() {
        stubStats(3, 3, 0);
        when(gameHistoryService.getFinishedGameStats(user.getId(),"RAPID")).thenReturn(List.of(
                game(user, opponent, GameStatus.WHITE_WIN, EndedBy.CHECKMATE),
                game(user, opponent, GameStatus.WHITE_WIN, EndedBy.TIMEOUT),
                game(user, opponent, GameStatus.WHITE_WIN, EndedBy.RESIGNATION)
        ));
        PlayerStatsDTO dto = userService.getPlayerStats("someUser", "RAPID");
        assertThat(dto.winsByCheckmate()).isEqualTo(1);
        assertThat(dto.winsByFlagging()).isEqualTo(1);
        assertThat(dto.winsByResignation()).isEqualTo(1);
    }

    @Test
    void getPlayerStatsShouldCountLossesByTerminationType() {
        stubStats(3, 0, 3);
        when(gameHistoryService.getFinishedGameStats(user.getId(),"RAPID")).thenReturn(List.of(
                game(opponent, user, GameStatus.WHITE_WIN, EndedBy.CHECKMATE),
                game(opponent, user, GameStatus.WHITE_WIN, EndedBy.TIMEOUT),
                game(opponent, user, GameStatus.WHITE_WIN, EndedBy.RESIGNATION)
        ));
        PlayerStatsDTO dto = userService.getPlayerStats("someUser", "RAPID");
        assertThat(dto.lossesByCheckmate()).isEqualTo(1);
        assertThat(dto.lossesByFlagging()).isEqualTo(1);
        assertThat(dto.lossesByResignation()).isEqualTo(1);
    }

    @Test
    void getPlayerStatsShouldCountDrawsByTerminationType() {
        stubStats(5, 0, 0);
        when(gameHistoryService.getFinishedGameStats(user.getId(),"RAPID")).thenReturn(List.of(
                game(user, opponent, GameStatus.DRAW, EndedBy.STALEMATE),
                game(user, opponent, GameStatus.DRAW, EndedBy.AGREEMENT),
                game(user, opponent, GameStatus.DRAW, EndedBy.REPETITION),
                game(user, opponent, GameStatus.DRAW, EndedBy.FIFTY_MOVE_RULE),
                game(user, opponent, GameStatus.DRAW, EndedBy.INSUFFICIENT_MATERIAL)
        ));
        PlayerStatsDTO dto = userService.getPlayerStats("someUser", "RAPID");
        assertThat(dto.drawsByStalemate()).isEqualTo(1);
        assertThat(dto.drawsByAgreement()).isEqualTo(1);
        assertThat(dto.drawsByRepetition()).isEqualTo(1);
        assertThat(dto.drawsBy50MoveRule()).isEqualTo(1);
        assertThat(dto.drawsByInsufficientMaterial()).isEqualTo(1);
    }

    @Test
    void getPlayerStatsShouldCountColorStats() {
        stubStats(4, 3, 1);
        when(gameHistoryService.getFinishedGameStats(user.getId(),"RAPID")).thenReturn(List.of(
                game(user, opponent, GameStatus.WHITE_WIN, EndedBy.CHECKMATE),
                game(user, opponent, GameStatus.BLACK_WIN, EndedBy.CHECKMATE),
                game(opponent, user, GameStatus.BLACK_WIN, EndedBy.CHECKMATE),
                game(opponent, user, GameStatus.WHITE_WIN, EndedBy.CHECKMATE)
        ));
        PlayerStatsDTO dto = userService.getPlayerStats("someUser", "RAPID");
        assertThat(dto.gamesAsWhite()).isEqualTo(2);
        assertThat(dto.winsAsWhite()).isEqualTo(1);
        assertThat(dto.lossesAsWhite()).isEqualTo(1);
        assertThat(dto.gamesAsBlack()).isEqualTo(2);
        assertThat(dto.winsAsBlack()).isEqualTo(1);
        assertThat(dto.lossesAsBlack()).isEqualTo(1);
    }

    @Test
    void getPlayerStatsShouldHandleNullEndedBy() {
        stubStats(1, 1, 0);
        when(gameHistoryService.getFinishedGameStats(user.getId(),"RAPID")).thenReturn(List.of(
                game(user, opponent, GameStatus.WHITE_WIN, null)
        ));
        PlayerStatsDTO dto = userService.getPlayerStats("someUser", "RAPID");
        assertThat(dto.winsByCheckmate()).isEqualTo(0);
        assertThat(dto.winsByFlagging()).isEqualTo(0);
        assertThat(dto.winsByResignation()).isEqualTo(0);
    }

}