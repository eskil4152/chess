package com.blikeng.chess.service;

import com.blikeng.chess.dto.websocket.WsCancelChallengeDTO;
import com.blikeng.chess.dto.websocket.WsChallengeDTO;
import com.blikeng.chess.dto.websocket.WsChallengeResponseDTO;
import com.blikeng.chess.entity.UserEntity;
import com.blikeng.chess.exception.types.InvalidChallengeException;
import com.blikeng.chess.exception.types.InvalidUserException;
import com.blikeng.chess.exception.types.NotFoundException;
import com.blikeng.chess.exception.types.UserNotFoundException;
import com.blikeng.chess.model.Challenge;
import com.blikeng.chess.model.timecontrol.TimeControl;
import com.blikeng.chess.notifications.NotificationService;
import com.blikeng.chess.dto.websocket.WsOutgoingChallengeCancelledDTO;
import com.blikeng.chess.dto.websocket.WsOutgoingChallengeDTO;
import com.blikeng.chess.dto.websocket.WsOutgoingChallengeResponseDTO;
import com.blikeng.chess.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChallengeService {
    private final UserRepository userRepository;
    private final GameService gameService;
    private final NotificationService notificationService;

    public ChallengeService(
        UserRepository userRepository,
        GameService gameService,
        NotificationService notificationService
    ){
        this.userRepository = userRepository;
        this.gameService = gameService;
        this.notificationService = notificationService;
    }

    private final ConcurrentHashMap<UUID, Challenge> challenges = new ConcurrentHashMap<>();

    @Scheduled(fixedRate = 60000L)
    private void clearStaleChallenges(){
        for (Challenge challenge : challenges.values()) {
            if (challenge.sent().isBefore(Instant.now().minusSeconds(600))) {
                challenges.remove(challenge.id());
                notificationService.onChallengeExpired(challenge.challengerId());
            }
        }
    }

    public void handleChallenge(UUID userId, WsChallengeDTO challengeDTO){
        // TODO: Deny if receiver is in game
        // TODO: Check if invite between 2 players already exists

        if (userId.equals(challengeDTO.receiver())) throw new InvalidChallengeException();

        UserEntity receiver = userRepository.findById(challengeDTO.receiver())
            .orElseThrow(UserNotFoundException::new);

        UserEntity sender = userRepository.findById(userId)
            .orElseThrow(InvalidUserException::new);

        TimeControl timeControl = TimeControl.fromName(challengeDTO.timeControl());

        Challenge challenge = new Challenge(
            UUID.randomUUID(),
            userId,
            receiver.getId(),
            timeControl,
            Instant.now()
        );

        challenges.put(challenge.id(), challenge);

        notificationService.onChallenge(
            challenge.challengedId(),
            new WsOutgoingChallengeDTO(challenge.id(), sender.getUsername(), timeControl.label())
        );
    }

    public void handleChallengeResponse(UUID userId, WsChallengeResponseDTO challengeResponseDTO){
        Challenge challenge = challenges.get(challengeResponseDTO.challengeId());
        if (challenge == null || !challenge.challengedId().equals(userId)) throw new NotFoundException();
        challenges.remove(challengeResponseDTO.challengeId());

        UserEntity challenged = userRepository.findById(userId)
            .orElseThrow(InvalidUserException::new);

        UserEntity challenger = userRepository.findById(challenge.challengerId())
            .orElseThrow(UserNotFoundException::new);

        if (challengeResponseDTO.accepted()) {
            gameService.beginGame(challenger, challenged, challenge.timeControl());
        } else {
            notificationService.onChallengeDeclined(
                challenger.getId(),
                new WsOutgoingChallengeResponseDTO(challenge.id(), challenged.getUsername())
            );
        }
    }

    public void cancelChallenge(UUID userId, WsCancelChallengeDTO cancelDTO){
        Challenge challenge = challenges.get(cancelDTO.challengeId());
        if (challenge == null || !challenge.challengerId().equals(userId)) throw new NotFoundException();
        challenges.remove(cancelDTO.challengeId());

        UserEntity challenger = userRepository.findById(userId)
            .orElseThrow(InvalidUserException::new);

        notificationService.onChallengeCancelled(
            challenge.challengedId(),
            new WsOutgoingChallengeCancelledDTO(challenge.id(), challenger.getUsername())
        );
    }
}
