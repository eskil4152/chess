package com.blikeng.chess.service;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenceService {
    private final ConcurrentHashMap<UUID, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    public void saveSession(UUID userId, WebSocketSession session) {
        userSessions.computeIfAbsent(userId, _ -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void removeSession(UUID userId, WebSocketSession session) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null) return;
        sessions.remove(session);
        if (sessions.isEmpty()) userSessions.remove(userId);
    }

    public boolean hasNoSessions(UUID userId) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        return sessions == null || sessions.isEmpty();
    }

    public Set<WebSocketSession> getSessions(UUID userId) {
        return userSessions.getOrDefault(userId, Set.of());
    }
}