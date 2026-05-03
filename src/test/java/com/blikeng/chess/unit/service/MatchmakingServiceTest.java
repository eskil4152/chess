package com.blikeng.chess.unit.service;

import com.blikeng.chess.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.blikeng.chess.service.AuthService;
import com.blikeng.chess.service.GameService;
import com.blikeng.chess.service.MatchmakingService;

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

    @Test
    void queuePlayerShouldAddToQueueWhenEmpty() {
        when(authService.findUserById(user1.getId())).thenReturn(Optional.of(user1));

        matchmakingService.queuePlayer(user1.getId());

        verify(gameService, never()).beginGame(any(), any());
    }

    @Test
    void queuePlayerShouldBeNoOpWhenAlreadyQueued() {
        when(authService.findUserById(user1.getId())).thenReturn(Optional.of(user1));

        matchmakingService.queuePlayer(user1.getId());
        matchmakingService.queuePlayer(user1.getId());

        verify(authService, times(2)).findUserById(user1.getId());
        verify(gameService, never()).beginGame(any(), any());
    }

    @Test
    void queuePlayerShouldStartGameWhenMatchFound() {
        user1.setElo(800);
        user2.setElo(850);

        when(authService.findUserById(user1.getId())).thenReturn(Optional.of(user1));
        when(authService.findUserById(user2.getId())).thenReturn(Optional.of(user2));

        matchmakingService.queuePlayer(user1.getId());
        matchmakingService.queuePlayer(user2.getId());

        verify(gameService).beginGame(user1, user2);
    }

    @Test
    void queuePlayerShouldNotMatchWhenEloDiffTooLarge() {
        user1.setElo(800);
        user2.setElo(1100);

        when(authService.findUserById(user1.getId())).thenReturn(Optional.of(user1));
        when(authService.findUserById(user2.getId())).thenReturn(Optional.of(user2));

        matchmakingService.queuePlayer(user1.getId());
        matchmakingService.queuePlayer(user2.getId());

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

        matchmakingService.queuePlayer(user1.getId()); // queued (empty)
        matchmakingService.queuePlayer(user2.getId()); // diff to user1 = 250 > 200, queued
        matchmakingService.queuePlayer(user3.getId()); // min picks user2 (100) over user1 (150)

        verify(gameService).beginGame(user2, user3);
        verify(gameService, never()).beginGame(eq(user1), any());
    }

    @Test
    void queuePlayerShouldThrowWhenUserNotFound() {
        when(authService.findUserById(user1.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> matchmakingService.queuePlayer(user1.getId()))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void dequeuePlayerShouldRemoveFromQueue() {
        when(authService.findUserById(user1.getId())).thenReturn(Optional.of(user1));
        when(authService.findUserById(user2.getId())).thenReturn(Optional.of(user2));
        user1.setElo(800);
        user2.setElo(810);

        matchmakingService.queuePlayer(user1.getId());
        matchmakingService.dequeuePlayer(user1.getId());
        matchmakingService.queuePlayer(user2.getId());

        verify(gameService, never()).beginGame(any(), any());
    }
}