package com.blikeng.chess.websocket;

import com.blikeng.chess.dto.websocket.WsMoveDTO;
import com.blikeng.chess.service.GameService;
import com.blikeng.chess.service.MatchmakingService;
import com.blikeng.chess.service.PresenceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.UUID;

@Component
public class WebSocketHandler extends TextWebSocketHandler {
    private final GameService gameService;
    private final MatchmakingService matchmakingService;
    private final PresenceService presenceService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WebSocketHandler(
            GameService gameService, MatchmakingService matchmakingService,
            PresenceService presenceService
    ) {
        this.gameService = gameService;
        this.matchmakingService = matchmakingService;
        this.presenceService = presenceService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        UUID userId = getUserId(session);
        presenceService.saveSession(userId, session);
        gameService.onSessionConnected(userId, session);
        if (!gameService.isInGame(userId)) {
            matchmakingService.queuePlayer(userId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        UUID userId = getUserId(session);

        try {
            JsonNode json = objectMapper.readTree(message.getPayload());
            String type = json.get("type").asText();

            switch (type) {
                case "MOVE" -> {
                    gameService.makeMove(userId, objectMapper.treeToValue(json, WsMoveDTO.class));
                }

                case "MESSAGE" -> {
                    // Send message
                }

                case "RESIGN" -> {
                    // Resign
                }

                case "OFFER_DRAW" -> {
                    // Offer draw
                }

                default -> {
                    /* ignore */
                }
            }
        } catch (Exception e) {
            // submit ws error
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID userId = getUserId(session);
        presenceService.removeSession(userId, session);
        if (presenceService.hasNoSessions(userId)) {
            matchmakingService.dequeuePlayer(userId);
        }
    }

    private UUID getUserId(WebSocketSession session) {
        return (UUID) session.getAttributes().get("userId");
    }
}
