package com.blikeng.chess.unit.service;

import com.blikeng.chess.dto.websocket.*;
import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.types.*;
import com.blikeng.chess.model.Challenge;
import com.blikeng.chess.model.timecontrol.TimeControl;
import com.blikeng.chess.notifications.NotificationService;
import com.blikeng.chess.repository.UserRepository;
import com.blikeng.chess.service.ChallengeService;
import com.blikeng.chess.service.GameService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceTest {

    @Mock UserRepository userRepository;
    @Mock GameService gameService;
    @Mock NotificationService notificationService;
    @InjectMocks ChallengeService challengeService;

    private UserEntity challenger;
    private UserEntity challenged;

    @BeforeEach
    void setup() {
        challenger = new UserEntity("challenger", "h");
        challenged = new UserEntity("challenged", "h");
    }

    // --- handleChallenge ---

    @Test
    void handleChallengeShouldStoreChallengeAndNotify() {
        when(userRepository.findById(challenged.getId())).thenReturn(Optional.of(challenged));
        when(userRepository.findById(challenger.getId())).thenReturn(Optional.of(challenger));

        challengeService.handleChallenge(challenger.getId(), new WsChallengeDTO(challenged.getId(), "BLITZ_5_0"));

        ConcurrentHashMap<?, ?> map = map();
        assertThat(map).hasSize(1);
        verify(notificationService).onChallenge(eq(challenger.getId()), eq(challenged.getId()), any(WsOutgoingChallengeDTO.class));
    }

    @Test
    void handleChallengeShouldThrowWhenSelfChallenge() {
        assertThatThrownBy(() ->
            challengeService.handleChallenge(challenger.getId(), new WsChallengeDTO(challenger.getId(), "BLITZ_5_0"))
        ).isInstanceOf(InvalidChallengeException.class);
    }

    @Test
    void handleChallengeShouldThrowWhenReceiverIsInGame() {
        when(userRepository.findById(challenged.getId())).thenReturn(Optional.of(challenged));
        when(gameService.isInGame(challenged.getId())).thenReturn(true);

        assertThatThrownBy(() ->
            challengeService.handleChallenge(challenger.getId(), new WsChallengeDTO(challenged.getId(), "BLITZ_5_0"))
        ).isInstanceOf(AlreadyInGameException.class);
    }

    @Test
    void handleChallengeShouldThrowWhenDuplicatePending() {
        seedChallenge(challenger.getId(), challenged.getId(), TimeControl.BLITZ_5_0, Instant.now());
        when(userRepository.findById(challenged.getId())).thenReturn(Optional.of(challenged));

        assertThatThrownBy(() ->
            challengeService.handleChallenge(challenger.getId(), new WsChallengeDTO(challenged.getId(), "BLITZ_5_0"))
        ).isInstanceOf(AlreadyChallengedException.class);
    }

    @Test
    void handleChallengeShouldAutoStartGameOnMutualChallenge() {
        Challenge existing = seedChallenge(challenged.getId(), challenger.getId(), TimeControl.BLITZ_5_0, Instant.now());
        when(userRepository.findById(challenged.getId())).thenReturn(Optional.of(challenged));
        when(userRepository.findById(challenger.getId())).thenReturn(Optional.of(challenger));

        challengeService.handleChallenge(challenger.getId(), new WsChallengeDTO(challenged.getId(), "BLITZ_3_0"));

        verify(gameService).beginGame(challenged, challenger, existing.timeControl());
        assertThat(map()).isEmpty();
        verify(notificationService, never()).onChallenge(any(), any(), any());
    }

    // --- handleChallengeResponse ---

    @Test
    void handleChallengeResponseShouldStartGameWhenAccepted() {
        Challenge challenge = seedChallenge(challenger.getId(), challenged.getId(), TimeControl.BLITZ_5_0, Instant.now());
        when(userRepository.findById(challenged.getId())).thenReturn(Optional.of(challenged));
        when(userRepository.findById(challenger.getId())).thenReturn(Optional.of(challenger));

        challengeService.handleChallengeResponse(challenged.getId(), new WsChallengeResponseDTO(WsMessageType.CHALLENGE_RESPONSE, challenge.id(), true));

        verify(gameService).beginGame(challenger, challenged, TimeControl.BLITZ_5_0);
        assertThat(map()).isEmpty();
    }

    @Test
    void handleChallengeResponseShouldNotifyDeclineAndRemoveChallenge() {
        Challenge challenge = seedChallenge(challenger.getId(), challenged.getId(), TimeControl.BLITZ_5_0, Instant.now());
        when(userRepository.findById(challenged.getId())).thenReturn(Optional.of(challenged));
        when(userRepository.findById(challenger.getId())).thenReturn(Optional.of(challenger));

        challengeService.handleChallengeResponse(challenged.getId(), new WsChallengeResponseDTO(WsMessageType.CHALLENGE_RESPONSE, challenge.id(), false));

        verify(notificationService).onChallengeDeclined(eq(challenger.getId()), any(WsOutgoingChallengeResponseDTO.class));
        assertThat(map()).isEmpty();
    }

    @Test
    void handleChallengeResponseShouldThrowWhenCallerIsNotChallenged() {
        Challenge challenge = seedChallenge(challenger.getId(), challenged.getId(), TimeControl.BLITZ_5_0, Instant.now());
        UUID intruder = UUID.randomUUID();

        assertThatThrownBy(() ->
            challengeService.handleChallengeResponse(intruder, new WsChallengeResponseDTO(WsMessageType.CHALLENGE_RESPONSE, challenge.id(), true))
        ).isInstanceOf(NotFoundException.class);

        assertThat(map()).hasSize(1);
    }

    @Test
    void handleChallengeResponseShouldThrowWhenChallengeNotFound() {
        assertThatThrownBy(() ->
            challengeService.handleChallengeResponse(challenged.getId(), new WsChallengeResponseDTO(WsMessageType.CHALLENGE_RESPONSE, UUID.randomUUID(), true))
        ).isInstanceOf(NotFoundException.class);
    }

    // --- cancelChallenge ---

    @Test
    void cancelChallengeShouldRemoveChallengeAndNotifyChallenged() {
        Challenge challenge = seedChallenge(challenger.getId(), challenged.getId(), TimeControl.BLITZ_5_0, Instant.now());
        when(userRepository.findById(challenger.getId())).thenReturn(Optional.of(challenger));

        challengeService.cancelChallenge(challenger.getId(), new WsCancelChallengeDTO(WsMessageType.CANCEL_CHALLENGE, challenge.id()));

        assertThat(map()).isEmpty();
        verify(notificationService).onChallengeCancelled(eq(challenged.getId()), any(WsOutgoingChallengeCancelledDTO.class));
    }

    @Test
    void cancelChallengeShouldThrowWhenCallerIsNotChallenger() {
        Challenge challenge = seedChallenge(challenger.getId(), challenged.getId(), TimeControl.BLITZ_5_0, Instant.now());

        assertThatThrownBy(() ->
            challengeService.cancelChallenge(challenged.getId(), new WsCancelChallengeDTO(WsMessageType.CANCEL_CHALLENGE, challenge.id()))
        ).isInstanceOf(NotFoundException.class);

        assertThat(map()).hasSize(1);
    }

    @Test
    void cancelChallengeShouldThrowWhenChallengeNotFound() {
        assertThatThrownBy(() ->
            challengeService.cancelChallenge(challenger.getId(), new WsCancelChallengeDTO(WsMessageType.CANCEL_CHALLENGE, UUID.randomUUID()))
        ).isInstanceOf(NotFoundException.class);
    }

    // --- clearStaleChallenges ---

    @Test
    void clearStaleChallengesShouldRemoveExpiredAndNotifyChallenger() {
        Challenge stale = seedChallenge(challenger.getId(), challenged.getId(), TimeControl.BLITZ_5_0, Instant.now().minusSeconds(700));

        ReflectionTestUtils.invokeMethod(challengeService, "clearStaleChallenges");

        assertThat(map()).isEmpty();
        verify(notificationService).onChallengeExpired(stale.challengerId());
    }

    @Test
    void clearStaleChallengesShouldRetainFreshChallenges() {
        seedChallenge(challenger.getId(), challenged.getId(), TimeControl.BLITZ_5_0, Instant.now());

        ReflectionTestUtils.invokeMethod(challengeService, "clearStaleChallenges");

        assertThat(map()).hasSize(1);
        verify(notificationService, never()).onChallengeExpired(any());
    }

    // --- helpers ---

    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<UUID, Challenge> map() {
        return (ConcurrentHashMap<UUID, Challenge>) ReflectionTestUtils.getField(challengeService, "challenges");
    }

    private Challenge seedChallenge(UUID challengerId, UUID challengedId, TimeControl tc, Instant sent) {
        Challenge c = new Challenge(UUID.randomUUID(), challengerId, challengedId, tc, sent);
        map().put(c.id(), c);
        return c;
    }
}
