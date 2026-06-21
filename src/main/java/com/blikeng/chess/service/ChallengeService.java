package com.blikeng.chess.service;

import com.blikeng.chess.dto.websocket.WsCancelChallengeDTO;
import com.blikeng.chess.dto.websocket.WsChallengeDTO;
import com.blikeng.chess.dto.websocket.WsChallengeResponseDTO;
import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.types.*;
import com.blikeng.chess.model.Challenge;
import com.blikeng.chess.model.timecontrol.TimeControl;
import com.blikeng.chess.dto.websocket.WsOutgoingChallengeCancelledDTO;
import com.blikeng.chess.dto.websocket.WsOutgoingChallengeDTO;
import com.blikeng.chess.dto.websocket.WsOutgoingChallengeResponseDTO;
import com.blikeng.chess.repository.UserRepository;
import com.blikeng.chess.service.game.ActiveGameStore;
import com.blikeng.chess.service.game.GameCreationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Direct player-to-player challenges: create, accept/decline, and cancel, with results
 * pushed via {@link NotificationService}.
 *
 * <p>Pending challenges are held in redis and expired a set constant.
 * <p>Every incoming challenge is checked for duplication and mirror. Mirrored invites auto accept.
 */
@Service
public class ChallengeService {
    private final UserRepository userRepository;
    private final ActiveGameStore activeGameStore;
    private final GameCreationService gameCreationService;
    private final NotificationService notificationService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final long CHALLENGE_EXPIRY_MINUTES = 5;

    public ChallengeService(
        UserRepository userRepository,
        ActiveGameStore activeGameStore,
        GameCreationService gameCreationService,
        NotificationService notificationService,
        RedisTemplate<String, String> redisTemplate
    ){
        this.userRepository = userRepository;
        this.activeGameStore = activeGameStore;
        this.gameCreationService = gameCreationService;
        this.notificationService = notificationService;
        this.redisTemplate = redisTemplate;
    }

    public void handleChallenge(UUID userId, WsChallengeDTO challengeDTO){
        if (userId.equals(challengeDTO.receiver())) throw new InvalidChallengeException();

        UserEntity receiver = userRepository.findById(challengeDTO.receiver())
            .orElseThrow(UserNotFoundException::new);

        if (activeGameStore.isInGame(receiver.getId())) throw new AlreadyInGameException();

        if (redisTemplate.hasKey("challenge:pair:" + userId + ":" + challengeDTO.receiver())) throw new AlreadyChallengedException();

        UserEntity sender = userRepository.findById(userId)
            .orElseThrow(InvalidUserException::new);

        String mutual = redisTemplate.opsForValue().get("challenge:pair:" + challengeDTO.receiver() + ":" + userId);

        if (mutual != null) {
            redisTemplate.delete("challenge:pair:" + challengeDTO.receiver() + ":" + userId);
            Challenge challenge;

            try {
                challenge = objectMapper.readValue(mutual, Challenge.class);
            } catch (Exception e) {
                // TODO: Custom exception and logger
                throw new IllegalStateException("Failed to serialize challenge", e);
            }

            gameCreationService.beginGame(receiver, sender, challenge.timeControl());
            return;
        }

        TimeControl timeControl = TimeControl.fromName(challengeDTO.timeControl());

        Challenge challenge = new Challenge(
            UUID.randomUUID(),
            userId,
            receiver.getId(),
            timeControl,
            Instant.now()
        );

        String json;
        try {
            json = objectMapper.writeValueAsString(challenge);
        } catch (Exception e) {
            // TODO: Custom exception and logger
            throw new IllegalStateException("Failed to serialize challenge", e);
        }

        redisTemplate.opsForValue().set("challenge:" + challenge.id(), json, Duration.ofMinutes(CHALLENGE_EXPIRY_MINUTES));
        redisTemplate.opsForValue().set("challenge:pair:" + userId + ":" + receiver.getId(), json, Duration.ofMinutes(CHALLENGE_EXPIRY_MINUTES));

        notificationService.onChallenge(
            challenge.challengerId(),
            challenge.challengedId(),
            new WsOutgoingChallengeDTO(challenge.id(), sender.getUsername(), timeControl.label())
        );
    }

    public void handleChallengeResponse(UUID userId, WsChallengeResponseDTO challengeResponseDTO){
        Challenge challenge = getChallenge(challengeResponseDTO.challengeId());

        if (challenge == null || !challenge.challengedId().equals(userId)) throw new NotFoundException();

        redisTemplate.delete("challenge:" + challenge.id());
        redisTemplate.delete("challenge:pair:" + challenge.challengerId() + ":" + challenge.challengedId());

        UserEntity challenged = userRepository.findById(userId)
            .orElseThrow(InvalidUserException::new);

        UserEntity challenger = userRepository.findById(challenge.challengerId())
            .orElseThrow(UserNotFoundException::new);

        if (challengeResponseDTO.accepted()) {
            gameCreationService.beginGame(challenger, challenged, challenge.timeControl());
        } else {
            notificationService.onChallengeDeclined(
                challenger.getId(),
                new WsOutgoingChallengeResponseDTO(challenge.id(), challenged.getUsername())
            );
        }
    }

    public void cancelChallenge(UUID userId, WsCancelChallengeDTO cancelDTO){
        Challenge challenge = getChallenge(cancelDTO.challengeId());

        if (challenge == null || !challenge.challengerId().equals(userId)) throw new NotFoundException();
        redisTemplate.delete("challenge:" + challenge.id());
        redisTemplate.delete("challenge:pair:" + challenge.challengerId() + ":" + challenge.challengedId());

        UserEntity challenger = userRepository.findById(userId)
            .orElseThrow(InvalidUserException::new);

        notificationService.onChallengeCancelled(
            challenge.challengedId(),
            new WsOutgoingChallengeCancelledDTO(challenge.id(), challenger.getUsername())
        );
    }

    private Challenge getChallenge(UUID challengeId){
        String challengeString = redisTemplate.opsForValue().get("challenge:" + challengeId);
        if (challengeString == null) throw new NotFoundException();

        try {
            return objectMapper.readValue(challengeString, Challenge.class);
        } catch (Exception e){
            // TODO: Custom exception and logger
            throw new IllegalStateException("Failed to serialize challenge", e);
        }
    }
}
