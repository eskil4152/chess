package com.blikeng.chess.messaging;

import com.blikeng.chess.dto.websocket.WsDrawDTO;
import com.blikeng.chess.dto.websocket.WsMoveDTO;
import com.blikeng.chess.dto.websocket.WsResignDTO;
import com.blikeng.chess.service.game.ActiveGameStore;
import com.blikeng.chess.service.game.GameService;
import com.blikeng.chess.service.game.GameViewService;
import jakarta.annotation.PostConstruct;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Subscribes to Redis channels matching {@code user:*} and forwards messages to the {@link LocalBroadcaster}.
 */
@Component
public class RedisMessageSubscriber implements MessageListener {
    private final LocalBroadcaster broadcaster;
    private final RedisMessageListenerContainer container;
    private final GameService gameService;
    private final ActiveGameStore activeGameStore;
    private final GameViewService gameViewService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Logger logger = LoggerFactory.getLogger(RedisMessageSubscriber.class);

    public RedisMessageSubscriber(
        LocalBroadcaster broadcaster,
        RedisMessageListenerContainer container,
        GameService gameService,
        ActiveGameStore activeGameStore,
        GameViewService gameViewService
    ) {
        this.broadcaster = broadcaster;
        this.container = container;
        this.gameService = gameService;
        this.activeGameStore = activeGameStore;
        this.gameViewService = gameViewService;
    }

    @PostConstruct
    public void init() {
        container.addMessageListener(this, new PatternTopic("user:*"));
        container.addMessageListener(this, new PatternTopic("game:*"));
    }

    @Override
    public void onMessage(Message message, byte @Nullable [] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        int separatorIndex = channel.indexOf(':');
        String prefix = separatorIndex >= 0 
            ? channel.substring(0, separatorIndex) 
            : channel;

        switch (prefix) {
            case "user" -> handleUserMessage(message, channel);
            case "game" -> handleGameMessage(message);
            default -> logger.warn("Unknown channel: {}", channel);
        }
    }

    private void handleUserMessage(Message message, String channel){
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        UUID userId;

        try {
            userId = UUID.fromString(channel.substring(5));
        } catch (IllegalArgumentException _) {
            return;
        }

        broadcaster.sendToUser(userId, payload);
    }

    private void handleGameMessage(Message message){
        try {
            JsonNode json = objectMapper.readTree(message.getBody());
            String gameId = json.path("gameId").asString();

            if (activeGameStore.get(gameId).isEmpty()) return;

            UUID userId = UUID.fromString(json.path("userId").asString());
            String action = json.path("action").asString();

            switch (action) {
                case "RESIGN" -> {
                    WsResignDTO resignDTO = new WsResignDTO(gameId);
                    gameService.resignGame(userId, resignDTO);
                }
                case "DRAW" -> {
                    WsDrawDTO drawDTO = new WsDrawDTO(gameId);
                    gameService.handleDraw(userId, drawDTO);
                }
                case "MOVE" -> {
                    WsMoveDTO moveDTO = new WsMoveDTO(gameId, json.path("move").asString(), null, null);
                    gameService.makeMove(userId, moveDTO);
                }
                case "RESTORE" -> gameViewService.pushGameState(gameId, userId);
                default -> logger.warn("Invalid action received: {}, for game {}", action, gameId);
            }
        } catch (JacksonException e) {
            logger.error("Failed to parse message: {}", message.getBody(), e);
            throw new RuntimeException(e);
        } catch (IllegalArgumentException _){
            logger.warn("Invalid message received: {}", message.getBody());
        }
    }
}
