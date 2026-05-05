package com.blikeng.chess.notifications;

import com.blikeng.chess.dto.websocket.WsDrawDTO;
import com.blikeng.chess.dto.websocket.WsGameEndedDTO;
import com.blikeng.chess.dto.websocket.WsGameStartedDTO;
import com.blikeng.chess.dto.websocket.WsMoveDTO;
import com.blikeng.chess.notifications.events.MatchEndedEvent;
import com.blikeng.chess.notifications.events.MatchStartedEvent;
import com.blikeng.chess.notifications.events.MoveMadeEvent;
import com.blikeng.chess.service.PresenceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {
    private final PresenceService presenceService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    public NotificationService(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMatchStarted(MatchStartedEvent event) {
        String payload = serialize(new WsGameStartedDTO(
                event.gameId(), event.whiteId(), event.whiteUsername(), event.blackId(), event.blackUsername()
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

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMatchEnded(MatchEndedEvent event) {
        String payload = serialize(new WsGameEndedDTO(event.gameId(), event.status(), event.endedBy()));
        sendToUser(event.whiteId(), payload);
        sendToUser(event.blackId(), payload);
    }

    public void sendDrawOffer(UUID gameId, UUID userId){
        String payload = serialize(new WsDrawDTO(gameId.toString()));
        sendToUser(userId, payload);
    }

    public void sendToSession(WebSocketSession session, String payload) {
        try {
            if (session.isOpen()) session.sendMessage(new TextMessage(payload));
        } catch (IOException e) {
            logger.warn("Error sending message to session: ", e);
        }
    }

    private void sendToUser(UUID userId, String payload) {
        presenceService.getSessions(userId).forEach(session -> sendToSession(session, payload));
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
