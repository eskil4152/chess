package com.blikeng.chess.websocket;

import com.blikeng.chess.dto.MoveDTO;
import com.blikeng.chess.service.GameService;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WebSocketHandler(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        UUID userId = getUserId(session);

        gameService.saveSession(userId, session);
        gameService.queuePlayer(userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        UUID userId = getUserId(session);

        try {
            JsonNode json = objectMapper.readTree(message.getPayload());
            String type = json.get("type").asText();

            switch (type) {
                case "MOVE" -> {
                    gameService.makeMove(userId, objectMapper.treeToValue(json, MoveDTO.class));
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
        gameService.removeSession(getUserId(session), session);
    }

    private UUID getUserId(WebSocketSession session) {
        return (UUID) session.getAttributes().get("userId");
    }
}
