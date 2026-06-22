package com.blikeng.chess.unit.service;

import com.blikeng.chess.dto.websocket.*;
import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.types.*;
import com.blikeng.chess.model.Challenge;
import com.blikeng.chess.model.timecontrol.TimeControl;
import com.blikeng.chess.service.NotificationService;
import com.blikeng.chess.repository.UserRepository;
import com.blikeng.chess.service.ChallengeService;
import com.blikeng.chess.service.game.ActiveGameStore;
import com.blikeng.chess.service.game.GameCreationService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceTest {

    @Mock UserRepository userRepository;
    @Mock ActiveGameStore activeGameStore;
    @Mock GameCreationService gameCreationService;
    @Mock NotificationService notificationService;
    @Mock RedisTemplate<String, String> redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    @InjectMocks ChallengeService challengeService;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        challengeService.handleChallenge(challenger.getId(), new WsChallengeDTO(challenged.getId(), "BLITZ_5_0"));

        verify(valueOps, times(2)).set(anyString(), anyString(), any(Duration.class));
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
        when(activeGameStore.isInGame(challenged.getId())).thenReturn(true);

        assertThatThrownBy(() ->
            challengeService.handleChallenge(challenger.getId(), new WsChallengeDTO(challenged.getId(), "BLITZ_5_0"))
        ).isInstanceOf(AlreadyInGameException.class);
    }

    @Test
    void handleChallengeShouldThrowWhenDuplicatePending() {
        when(userRepository.findById(challenged.getId())).thenReturn(Optional.of(challenged));
        when(redisTemplate.hasKey("challenge:pair:" + challenger.getId() + ":" + challenged.getId())).thenReturn(true);

        assertThatThrownBy(() ->
            challengeService.handleChallenge(challenger.getId(), new WsChallengeDTO(challenged.getId(), "BLITZ_5_0"))
        ).isInstanceOf(AlreadyChallengedException.class);
    }

    @Test
    void handleChallengeShouldAutoStartGameOnMutualChallenge() {
        Challenge existing = new Challenge(UUID.randomUUID(), challenged.getId(), challenger.getId(), TimeControl.BLITZ_5_0, Instant.now());
        String mutualKey = "challenge:pair:" + challenged.getId() + ":" + challenger.getId();

        when(userRepository.findById(challenged.getId())).thenReturn(Optional.of(challenged));
        when(userRepository.findById(challenger.getId())).thenReturn(Optional.of(challenger));
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(mutualKey)).thenReturn(json(existing));

        challengeService.handleChallenge(challenger.getId(), new WsChallengeDTO(challenged.getId(), "BLITZ_3_0"));

        verify(gameCreationService).beginGame(challenged, challenger, existing.timeControl());
        verify(redisTemplate).delete(mutualKey);
        verify(notificationService, never()).onChallenge(any(), any(), any());
    }

    // --- handleChallengeResponse ---

    @Test
    void handleChallengeResponseShouldStartGameWhenAccepted() {
        Challenge challenge = new Challenge(UUID.randomUUID(), challenger.getId(), challenged.getId(), TimeControl.BLITZ_5_0, Instant.now());
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("challenge:" + challenge.id())).thenReturn(json(challenge));
        when(userRepository.findById(challenged.getId())).thenReturn(Optional.of(challenged));
        when(userRepository.findById(challenger.getId())).thenReturn(Optional.of(challenger));

        challengeService.handleChallengeResponse(challenged.getId(), new WsChallengeResponseDTO(WsMessageType.CHALLENGE_RESPONSE, challenge.id(), true));

        verify(gameCreationService).beginGame(challenger, challenged, TimeControl.BLITZ_5_0);
        verify(redisTemplate).delete("challenge:" + challenge.id());
    }

    @Test
    void handleChallengeResponseShouldNotifyDeclineAndRemoveChallenge() {
        Challenge challenge = new Challenge(UUID.randomUUID(), challenger.getId(), challenged.getId(), TimeControl.BLITZ_5_0, Instant.now());
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("challenge:" + challenge.id())).thenReturn(json(challenge));
        when(userRepository.findById(challenged.getId())).thenReturn(Optional.of(challenged));
        when(userRepository.findById(challenger.getId())).thenReturn(Optional.of(challenger));

        challengeService.handleChallengeResponse(challenged.getId(), new WsChallengeResponseDTO(WsMessageType.CHALLENGE_RESPONSE, challenge.id(), false));

        verify(notificationService).onChallengeDeclined(eq(challenger.getId()), any(WsOutgoingChallengeResponseDTO.class));
        verify(redisTemplate).delete("challenge:" + challenge.id());
    }

    @Test
    void handleChallengeResponseShouldThrowWhenCallerIsNotChallenged() {
        Challenge challenge = new Challenge(UUID.randomUUID(), challenger.getId(), challenged.getId(), TimeControl.BLITZ_5_0, Instant.now());
        UUID intruder = UUID.randomUUID();
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("challenge:" + challenge.id())).thenReturn(json(challenge));

        assertThatThrownBy(() ->
            challengeService.handleChallengeResponse(intruder, new WsChallengeResponseDTO(WsMessageType.CHALLENGE_RESPONSE, challenge.id(), true))
        ).isInstanceOf(NotFoundException.class);

        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void handleChallengeResponseShouldThrowWhenChallengeNotFound() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        assertThatThrownBy(() ->
            challengeService.handleChallengeResponse(challenged.getId(), new WsChallengeResponseDTO(WsMessageType.CHALLENGE_RESPONSE, UUID.randomUUID(), true))
        ).isInstanceOf(NotFoundException.class);
    }

    // --- cancelChallenge ---

    @Test
    void cancelChallengeShouldRemoveChallengeAndNotifyChallenged() {
        Challenge challenge = new Challenge(UUID.randomUUID(), challenger.getId(), challenged.getId(), TimeControl.BLITZ_5_0, Instant.now());
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("challenge:" + challenge.id())).thenReturn(json(challenge));
        when(userRepository.findById(challenger.getId())).thenReturn(Optional.of(challenger));

        challengeService.cancelChallenge(challenger.getId(), new WsCancelChallengeDTO(WsMessageType.CANCEL_CHALLENGE, challenge.id()));

        verify(redisTemplate).delete("challenge:" + challenge.id());
        verify(notificationService).onChallengeCancelled(eq(challenged.getId()), any(WsOutgoingChallengeCancelledDTO.class));
    }

    @Test
    void cancelChallengeShouldThrowWhenCallerIsNotChallenger() {
        Challenge challenge = new Challenge(UUID.randomUUID(), challenger.getId(), challenged.getId(), TimeControl.BLITZ_5_0, Instant.now());
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("challenge:" + challenge.id())).thenReturn(json(challenge));

        assertThatThrownBy(() ->
            challengeService.cancelChallenge(challenged.getId(), new WsCancelChallengeDTO(WsMessageType.CANCEL_CHALLENGE, challenge.id()))
        ).isInstanceOf(NotFoundException.class);

        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void cancelChallengeShouldThrowWhenChallengeNotFound() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        assertThatThrownBy(() ->
            challengeService.cancelChallenge(challenger.getId(), new WsCancelChallengeDTO(WsMessageType.CANCEL_CHALLENGE, UUID.randomUUID()))
        ).isInstanceOf(NotFoundException.class);
    }

    // --- helpers ---

    private String json(Challenge challenge) {
        try {
            return objectMapper.writeValueAsString(challenge);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
