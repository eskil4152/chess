package com.blikeng.chess.messaging;

import com.blikeng.chess.service.PresenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

import java.util.UUID;
import java.util.concurrent.Executor;

@Component
public class LocalBroadcaster {
    private final Executor executor;
    private final PresenceService presenceService;

    public LocalBroadcaster(@Qualifier("applicationTaskExecutor") Executor executor, PresenceService presenceService) {
        this.executor = executor;
        this.presenceService = presenceService;
    }

    private final Logger logger = LoggerFactory.getLogger(LocalBroadcaster.class);

    public void sendToUser(UUID userId, String payload) {
        TextMessage message = new TextMessage(payload);
        for (var session : presenceService.getSessions(userId)) {
            executor.execute(() -> {
                synchronized (session) {
                    if (session.isOpen()){
                        try {
                            session.sendMessage(message);
                        } catch (Exception e) {
                            logger.error("Failed to send message to user: {}", userId, e);
                            presenceService.removeSession(userId, session);
                            try { session.close(); } catch (Exception _) { /* ignore */ }
                        }
                    }
                }
            });
        }
    }
}
