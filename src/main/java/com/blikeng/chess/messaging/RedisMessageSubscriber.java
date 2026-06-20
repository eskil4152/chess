package com.blikeng.chess.messaging;

import com.blikeng.chess.dto.websocket.WsDrawDTO;
import com.blikeng.chess.dto.websocket.WsMoveDTO;
import com.blikeng.chess.dto.websocket.WsResignDTO;
import com.blikeng.chess.service.game.ActiveGameStore;
import com.blikeng.chess.service.game.GameService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.io.IOException;
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

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Logger logger = LoggerFactory.getLogger(RedisMessageSubscriber.class);

    public RedisMessageSubscriber(
        LocalBroadcaster broadcaster,
        RedisMessageListenerContainer container,
        GameService gameService,
        ActiveGameStore activeGameStore
    ) {
        this.broadcaster = broadcaster;
        this.container = container;
        this.gameService = gameService;
        this.activeGameStore = activeGameStore;
    }

    @PostConstruct
    public void init() {
        container.addMessageListener(this, new PatternTopic("user:*"));
        container.addMessageListener(this, new PatternTopic("game:*"));
    }

    @Override
    public void onMessage(Message message, byte @Nullable [] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);

        if (channel.startsWith("user")){
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            UUID userId;

            try {
                userId = UUID.fromString(channel.substring(5));
            } catch (IllegalArgumentException _) {
                return;
            }

            broadcaster.sendToUser(userId, payload);
        } else {
            try {
                JsonNode json = objectMapper.readTree(message.getBody());
                String gameId = json.path("gameId").asText();

                if (activeGameStore.get(gameId).isEmpty()) return;

                UUID userId = UUID.fromString(json.path("userId").asText());
                String action = json.path("action").asText();

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
                        WsMoveDTO moveDTO = new WsMoveDTO(gameId, json.path("move").asText(), null, null);
                        gameService.makeMove(userId, moveDTO);
                    }
                    default -> {
                        logger.warn("Invalid action received: {}, for game {}", action, gameId);
                    }
                }
            } catch (IOException e) {
                logger.error("Failed to parse message: {}", message.getBody(), e);
                throw new RuntimeException(e);
            } catch (IllegalArgumentException _){
                logger.warn("Invalid message received: {}", message.getBody());
            }
        }
    }
}
