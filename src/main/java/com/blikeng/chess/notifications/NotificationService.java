package com.blikeng.chess.notifications;

import com.blikeng.chess.dto.websocket.*;
import com.blikeng.chess.notifications.events.*;
import com.blikeng.chess.service.PresenceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class NotificationService {
    private final PresenceService presenceService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private final ConcurrentHashMap<String, ReentrantLock> sessionLocks = new ConcurrentHashMap<>();

    public NotificationService(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMatchStarted(MatchStartedEvent event) {
        String payload = serialize(new WsGameStartedDTO(
                event.gameId(), event.whiteId(), event.whiteUsername(), event.blackId(), event.blackUsername(), event.whiteElo(), event.blackElo()
        ));
        sendToUser(event.whiteId(), payload);
        sendToUser(event.blackId(), payload);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMoveMade(MoveMadeEvent event) {
        String payload = serialize(new WsMoveDTO(event.gameId().toString(), event.move()));
        sendToUser(event.whiteId(), payload);
        sendToUser(event.blackId(), payload);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onMatchEnded(MatchEndedEvent event) {
        String payload = serialize(new WsGameEndedDTO(event.gameId(), event.status(), event.endedBy(), event.whiteElo(), event.blackElo()));
        sendToUser(event.whiteId(), payload);
        sendToUser(event.blackId(), payload);
    }

    public void onChallenge(UUID challengedId, WsOutgoingChallengeDTO dto){
        sendToUser(challengedId, serialize(dto));
    }

    public void onChallengeCancelled(UUID challengedId, WsOutgoingChallengeCancelledDTO dto){
        sendToUser(challengedId, serialize(dto));
    }

    public void onChallengeDeclined(UUID challengerId, WsOutgoingChallengeResponseDTO dto){
        sendToUser(challengerId, serialize(dto));
    }

    public void sendDrawOffer(UUID gameId, UUID userId){
        String payload = serialize(new WsDrawDTO(gameId.toString()));
        sendToUser(userId, payload);
    }

    public void sendToSession(WebSocketSession session, String payload) {
        ReentrantLock lock = sessionLocks.computeIfAbsent(session.getId(), _ -> new ReentrantLock());
        lock.lock();
        try {
            if (session.isOpen()) session.sendMessage(new TextMessage(payload));
        } catch (IOException e) {
            logger.warn("Error sending message to session: ", e);
        } finally {
            lock.unlock();
        }
    }

    @Scheduled(fixedRate = 20000)
    public void pingAllSessions() {
        PingMessage ping = new PingMessage();
        presenceService.getAllSessions().forEach(session -> {
            ReentrantLock lock = sessionLocks.computeIfAbsent(session.getId(), _ -> new ReentrantLock());
            lock.lock();
            try {
                if (session.isOpen()) {
                    session.sendMessage(ping);
                } else {
                    UUID userId = (UUID) session.getAttributes().get("userId");
                    presenceService.removeSession(userId, session);
                    sessionLocks.remove(session.getId());
                }
            } catch (IOException e) {
                logger.warn("Ping failed for session {}: {}", session.getId(), e.getMessage());
            } finally {
                lock.unlock();
            }
        });
    }

    public void removeSession(String sessionId) {
        sessionLocks.remove(sessionId);
    }

    private void sendToUser(UUID userId, String payload) {
        presenceService.getSessions(userId).forEach(session -> sendToSession(session, payload));
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize object", e);
        }
    }
}
