package com.blikeng.chess.service;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceService {
    private final ConcurrentHashMap<UUID, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    public void saveSession(UUID userId, WebSocketSession session) {
        userSessions.compute(userId, (k, sessions) -> {
            if (sessions == null) sessions = ConcurrentHashMap.newKeySet();
            sessions.add(session);
            return sessions;
        });
    }

    public void removeSession(UUID userId, WebSocketSession session) {
        userSessions.computeIfPresent(userId, (k, sessions) -> {
            sessions.remove(session);
            return sessions.isEmpty() ? null : sessions;
        });
    }

    public boolean hasNoSessions(UUID userId) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        return sessions == null;
    }

    public Set<WebSocketSession> getSessions(UUID userId) {
        return userSessions.getOrDefault(userId, Set.of());
    }

    public Collection<WebSocketSession> getAllSessions() {
        return userSessions.values().stream()
                .flatMap(Collection::stream)
                .toList();
    }
}