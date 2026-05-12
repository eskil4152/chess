package com.blikeng.chess.unit.service;

import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.types.ExistingGameException;
import com.blikeng.chess.exception.types.InvalidUserException;
import com.blikeng.chess.security.JwtPrincipal;
import com.blikeng.chess.security.UserRole;
import com.blikeng.chess.service.AuthService;
import com.blikeng.chess.service.GameService;
import com.blikeng.chess.service.MatchmakingService;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchmakingServiceTest {

    @Mock AuthService authService;
    @Mock GameService gameService;
    @InjectMocks MatchmakingService matchmakingService;

    private UserEntity user1;
    private UserEntity user2;

    @BeforeEach
    void setup() {
        user1 = new UserEntity("user1", "h");
        user2 = new UserEntity("user2", "h");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setupSecurityContext(UUID userId) {
        var principal = new JwtPrincipal(userId, "testuser", UserRole.USER);
        var auth = new UsernamePasswordAuthenticationToken(principal, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void queuePlayerShouldAddToQueueWhenEmpty() {
        setupSecurityContext(user1.getId());
        when(authService.findUserById(user1.getId())).thenReturn(Optional.of(user1));

        matchmakingService.queuePlayer();

        verify(gameService, never()).beginGame(any(), any());
    }

    @Test
    void queuePlayerShouldBeNoOpWhenAlreadyQueued() {
        setupSecurityContext(user1.getId());
        when(authService.findUserById(user1.getId())).thenReturn(Optional.of(user1));

        matchmakingService.queuePlayer();
        matchmakingService.queuePlayer();

        verify(authService, times(2)).findUserById(user1.getId());
        verify(gameService, never()).beginGame(any(), any());
    }

    @Test
    void queuePlayerShouldStartGameWhenMatchFound() {
        user1.setElo(800);
        user2.setElo(850);

        when(authService.findUserById(user1.getId())).thenReturn(Optional.of(user1));
        when(authService.findUserById(user2.getId())).thenReturn(Optional.of(user2));

        setupSecurityContext(user1.getId());
        matchmakingService.queuePlayer();
        setupSecurityContext(user2.getId());
        matchmakingService.queuePlayer();

        verify(gameService).beginGame(user1, user2);
    }

    @Test
    void queuePlayerShouldNotMatchWhenEloDiffTooLarge() {
        user1.setElo(800);
        user2.setElo(1100);

        when(authService.findUserById(user1.getId())).thenReturn(Optional.of(user1));
        when(authService.findUserById(user2.getId())).thenReturn(Optional.of(user2));

        setupSecurityContext(user1.getId());
        matchmakingService.queuePlayer();
        setupSecurityContext(user2.getId());
        matchmakingService.queuePlayer();

        verify(gameService, never()).beginGame(any(), any());
    }

    @Test
    void queuePlayerShouldMatchClosestEloFromMultipleCandidates() {
        UserEntity user3 = new UserEntity("user3", "h");
        user1.setElo(500);  // diff to user3(650) = 150
        user2.setElo(750);  // diff to user3(650) = 100  ← closer, should be matched
        user3.setElo(650);

        when(authService.findUserById(user1.getId())).thenReturn(Optional.of(user1));
        when(authService.findUserById(user2.getId())).thenReturn(Optional.of(user2));
        when(authService.findUserById(user3.getId())).thenReturn(Optional.of(user3));

        setupSecurityContext(user1.getId());
        matchmakingService.queuePlayer(); // queued (empty)
        setupSecurityContext(user2.getId());
        matchmakingService.queuePlayer(); // diff to user1 = 250 > 200, queued
        setupSecurityContext(user3.getId());
        matchmakingService.queuePlayer(); // min picks user2 (100) over user1 (150)

        verify(gameService).beginGame(user2, user3);
        verify(gameService, never()).beginGame(eq(user1), any());
    }

    @Test
    void queuePlayerShouldThrowWhenUserNotFound() {
        setupSecurityContext(user1.getId());
        when(authService.findUserById(user1.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> matchmakingService.queuePlayer())
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void queuePlayerShouldThrowWhenUserIsAlreadyInGame() {
        setupSecurityContext(user1.getId());
        when(authService.findUserById(user1.getId())).thenReturn(Optional.of(user1));
        when(gameService.isInGame(user1.getId())).thenReturn(true);
        assertThatThrownBy(() -> matchmakingService.queuePlayer())
                .isInstanceOf(ExistingGameException.class);
    }

    @Test
    void queuePlayerShouldThrowWhenNotAuthenticated() {
        assertThatThrownBy(() -> matchmakingService.queuePlayer())
                .isInstanceOf(InvalidUserException.class);
    }

    @Test
    void dequeuePlayerShouldRemoveFromQueue() {
        user1.setElo(800);
        user2.setElo(810);
        when(authService.findUserById(user1.getId())).thenReturn(Optional.of(user1));
        when(authService.findUserById(user2.getId())).thenReturn(Optional.of(user2));

        setupSecurityContext(user1.getId());
        matchmakingService.queuePlayer();
        matchmakingService.dequeuePlayer(user1.getId());
        setupSecurityContext(user2.getId());
        matchmakingService.queuePlayer();

        verify(gameService, never()).beginGame(any(), any());
    }

    @Test
    void dequeuePlayerByHttpShouldRemoveFromQueue() {
        user1.setElo(800);
        user2.setElo(810);
        when(authService.findUserById(user1.getId())).thenReturn(Optional.of(user1));
        when(authService.findUserById(user2.getId())).thenReturn(Optional.of(user2));

        setupSecurityContext(user1.getId());
        matchmakingService.queuePlayer();
        matchmakingService.dequeuePlayer();
        setupSecurityContext(user2.getId());
        matchmakingService.queuePlayer();

        verify(gameService, never()).beginGame(any(), any());
    }

    @Test
    void dequeuePlayerByHttpShouldThrowWhenNotAuthenticated() {
        assertThatThrownBy(() -> matchmakingService.dequeuePlayer())
                .isInstanceOf(InvalidUserException.class);
    }
}