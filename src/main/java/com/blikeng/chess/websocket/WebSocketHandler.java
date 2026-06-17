package com.blikeng.chess.websocket;

import com.blikeng.chess.dto.websocket.*;
import com.blikeng.chess.exception.ApiException;
import com.blikeng.chess.service.NotificationService;
import com.blikeng.chess.service.ChallengeService;
import com.blikeng.chess.service.GameService;
import com.blikeng.chess.service.MatchmakingService;
import com.blikeng.chess.service.PresenceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final NotificationService notificationService;
    private final ChallengeService challengeService;
    private final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);

    public WebSocketHandler(
            GameService gameService,
            MatchmakingService matchmakingService,
            PresenceService presenceService,
            NotificationService notificationService,
            ChallengeService challengeService
    ) {
        this.gameService = gameService;
        this.matchmakingService = matchmakingService;
        this.presenceService = presenceService;
        this.notificationService = notificationService;
        this.challengeService = challengeService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        UUID userId = getUserId(session);
        presenceService.saveSession(userId, session);

        logger.debug("New connection for user: {}", userId);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        UUID userId = getUserId(session);

        try {
            JsonNode json = objectMapper.readTree(message.getPayload());
            String type = json.path("type").asText("");

            switch (type) {
                case "MOVE" -> gameService.makeMove(userId, objectMapper.treeToValue(json, WsMoveDTO.class));

                case "RESIGN" -> gameService.resignGame(userId, objectMapper.treeToValue(json, WsResignDTO.class));

                case "OFFER_DRAW" -> gameService.handleDraw(userId, objectMapper.treeToValue(json, WsDrawDTO.class));

                case "CHALLENGE" -> challengeService.handleChallenge(userId, objectMapper.treeToValue(json, WsChallengeDTO.class));

                case "CHALLENGE_RESPONSE" -> challengeService.handleChallengeResponse(userId, objectMapper.treeToValue(json, WsChallengeResponseDTO.class));

                case "CANCEL_CHALLENGE" -> challengeService.cancelChallenge(userId, objectMapper.treeToValue(json, WsCancelChallengeDTO.class));

                default -> { /* ignore */ }
            }
        } catch (ApiException e) {
           sendError(session, e.getStatus().value(), e.getMessage());
        } catch (Exception e) {
            logger.error("WS Handler error for user {}: ", userId, e);
            sendError(session, 500, "Internal server error");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID userId = getUserId(session);
        presenceService.removeSession(userId, session);
        notificationService.removeSession(session.getId());
        if (presenceService.hasNoSessions(userId)) {
            matchmakingService.dequeuePlayer(userId);
        }

        logger.info("Connection closed for user: {} — code={} reason={}", userId, status.getCode(), status.getReason());
    }

    private UUID getUserId(WebSocketSession session) {
        return (UUID) session.getAttributes().get("userId");
    }

    private void sendError(WebSocketSession session, int status, String message) {
        try {
            notificationService.sendToSession(session,
                    objectMapper.writeValueAsString(new WsErrorDTO(status, message)));
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize message: {}", message, e);
        }
    }
}
