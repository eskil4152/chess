package com.blikeng.chess.messaging;

import jakarta.annotation.PostConstruct;
import org.jspecify.annotations.Nullable;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RedisMessageSubscriber implements MessageListener {
    private final LocalBroadcaster broadcaster;
    private final RedisMessageListenerContainer container;

    public RedisMessageSubscriber(LocalBroadcaster broadcaster, RedisMessageListenerContainer container) {
        this.broadcaster = broadcaster;
        this.container = container;
    }

    @PostConstruct
    public void init() {
        container.addMessageListener(this, new PatternTopic("user:*"));
    }

    @Override
    public void onMessage(Message message, byte @Nullable [] pattern) {
        String payload = message.getBody().toString();
        String channel = message.getChannel().toString();
        UUID userId;

        try {
            userId = UUID.fromString(channel.substring(5));
        } catch (IllegalArgumentException _) {
            return;
        }

        // broadcaster.sendToUser(userId, payload);
    }
}
