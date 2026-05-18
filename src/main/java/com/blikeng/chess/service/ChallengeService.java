package com.blikeng.chess.service;

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
import com.blikeng.chess.notifications.events.ChallengeEvent;
import com.blikeng.chess.notifications.events.ChallengeDeclinedEvent;
import com.blikeng.chess.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

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

    public void handleChallenge(UUID userId, WsChallengeDTO challengeDTO){
        // TODO: Deny if receiver is in game

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
            timeControl
        );

        challenges.put(challenge.id(), challenge);

        notificationService.onChallenge(new ChallengeEvent(
            challenge.id(),
            userId,
            sender.getUsername(),
            challengeDTO.receiver(),
            timeControl.label()
        ));
    }

    public void handleChallengeResponse(UUID userId, WsChallengeResponseDTO challengeResponseDTO){
        Challenge challenge = challenges.remove(challengeResponseDTO.challengeId());
        if (challenge == null) throw new NotFoundException();

        UserEntity user = userRepository.findById(userId)
            .orElseThrow(InvalidUserException::new);

        if (!challenge.receiverId().equals(userId)) throw new NotFoundException();

        UserEntity challenger = userRepository.findById(challenge.senderId())
            .orElseThrow(UserNotFoundException::new);

        if (challengeResponseDTO.accepted()) {
            gameService.beginGame(challenger, user, challenge.timeControl());
        } else {
            notificationService.onChallengeDeclined(new ChallengeDeclinedEvent(
                challenge.id(),
                challenger.getId(),
                user.getUsername()
            ));
        }
    }

    public void cancelChallenge(){}
}
