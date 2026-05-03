package com.blikeng.chess.unit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import com.blikeng.chess.service.PresenceService;

class PresenceServiceTest {

    private PresenceService presenceService;
    private UUID userId;

    @BeforeEach
    void setup() {
        presenceService = new PresenceService();
        userId = UUID.randomUUID();
    }

    @Test
    void saveSessionShouldCreateEntryForFirstSession() {
        WebSocketSession session = mock(WebSocketSession.class);
        presenceService.saveSession(userId, session);
        assertThat(presenceService.getSessions(userId)).containsExactly(session);
        assertThat(presenceService.hasNoSessions(userId)).isFalse();
    }

    @Test
    void saveSessionShouldAppendAllSessionsForSameUser() {
        WebSocketSession s1 = mock(WebSocketSession.class);
        WebSocketSession s2 = mock(WebSocketSession.class);
        presenceService.saveSession(userId, s1);
        presenceService.saveSession(userId, s2);
        assertThat(presenceService.getSessions(userId)).containsExactlyInAnyOrder(s1, s2);
    }

    @Test
    void removeSessionShouldKeepEntryWhenOtherSessionsRemain() {
        WebSocketSession s1 = mock(WebSocketSession.class);
        WebSocketSession s2 = mock(WebSocketSession.class);
        presenceService.saveSession(userId, s1);
        presenceService.saveSession(userId, s2);
        presenceService.removeSession(userId, s1);
        assertThat(presenceService.getSessions(userId)).containsExactly(s2);
        assertThat(presenceService.hasNoSessions(userId)).isFalse();
    }

    @Test
    void removeSessionShouldRemoveEntryWhenLastSession() {
        WebSocketSession session = mock(WebSocketSession.class);
        presenceService.saveSession(userId, session);
        presenceService.removeSession(userId, session);
        assertThat(presenceService.hasNoSessions(userId)).isTrue();
        assertThat(presenceService.getSessions(userId)).isEmpty();
    }

    @Test
    void removeSessionShouldBeNoOpWhenUserNotPresent() {
        WebSocketSession session = mock(WebSocketSession.class);
        presenceService.removeSession(userId, session);
        assertThat(presenceService.hasNoSessions(userId)).isTrue();
    }

    @Test
    void hasNoSessionsShouldReturnTrueWhenUserNotInMap() {
        assertThat(presenceService.hasNoSessions(UUID.randomUUID())).isTrue();
    }

    @Test
    void getSessionsShouldReturnEmptySetWhenUserNotInMap() {
        assertThat(presenceService.getSessions(UUID.randomUUID())).isEmpty();
    }
}