package com.blikeng.chess.unit.notifications;

import com.blikeng.chess.dto.websocket.*;
import com.blikeng.chess.model.EndedBy;
import com.blikeng.chess.model.GameStatus;
import com.blikeng.chess.service.NotificationService;
import com.blikeng.chess.events.MatchEndedEvent;
import com.blikeng.chess.events.MatchStartedEvent;
import com.blikeng.chess.events.MoveMadeEvent;
import com.blikeng.chess.service.PresenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock PresenceService presenceService;
    @InjectMocks NotificationService notificationService;

    private UUID whiteId;
    private UUID blackId;
    private UUID gameId;

    @BeforeEach
    void setup() {
        whiteId = UUID.randomUUID();
        blackId = UUID.randomUUID();
        gameId = UUID.randomUUID();
    }

    private WebSocketSession openSession() {
        WebSocketSession s = mock(WebSocketSession.class);
        when(s.getId()).thenReturn(UUID.randomUUID().toString());
        when(s.isOpen()).thenReturn(true);
        return s;
    }

    private WebSocketSession closedSession() {
        WebSocketSession s = mock(WebSocketSession.class);
        when(s.getId()).thenReturn(UUID.randomUUID().toString());
        when(s.isOpen()).thenReturn(false);
        return s;
    }

    @Test
    void shouldSendMatchStartedToBothPlayers() throws IOException {
        WebSocketSession ws = openSession();
        WebSocketSession bs = openSession();
        when(presenceService.getSessions(whiteId)).thenReturn(Set.of(ws));
        when(presenceService.getSessions(blackId)).thenReturn(Set.of(bs));

        notificationService.onMatchStarted(new MatchStartedEvent(gameId, whiteId, "white", blackId, "black", 100, 100));

        verify(ws).sendMessage(any(TextMessage.class));
        verify(bs).sendMessage(any(TextMessage.class));
    }

    @Test
    void shouldSendMoveMadeToBothPlayers() throws IOException {
        WebSocketSession ws = openSession();
        WebSocketSession bs = openSession();
        when(presenceService.getSessions(whiteId)).thenReturn(Set.of(ws));
        when(presenceService.getSessions(blackId)).thenReturn(Set.of(bs));

        notificationService.onMoveMade(new MoveMadeEvent(gameId, whiteId, blackId, "e2e4", true, 0, Set.of()));

        verify(ws).sendMessage(any(TextMessage.class));
        verify(bs).sendMessage(any(TextMessage.class));
    }

    @Test
    void shouldSendMatchEndedToBothPlayers() throws IOException {
        WebSocketSession ws = openSession();
        WebSocketSession bs = openSession();
        when(presenceService.getSessions(whiteId)).thenReturn(Set.of(ws));
        when(presenceService.getSessions(blackId)).thenReturn(Set.of(bs));

        notificationService.onMatchEnded(new MatchEndedEvent(gameId, whiteId, blackId, GameStatus.WHITE_WIN, EndedBy.CHECKMATE, 100, 100, Set.of()));

        verify(ws).sendMessage(any(TextMessage.class));
        verify(bs).sendMessage(any(TextMessage.class));
    }

    @Test
    void openSessionShouldReceiveMessage() throws IOException {
        WebSocketSession session = openSession();
        notificationService.sendToSession(session, "hello");
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        assertThat(captor.getValue().getPayload()).isEqualTo("hello");
    }

    @Test
    void closedSessionShouldNotReceiveMessage() throws IOException {
        WebSocketSession closed = closedSession();
        notificationService.sendToSession(closed, "hello");
        verify(closed, never()).sendMessage(any());
    }

    @Test
    void ioExceptionShouldNotPropagate() throws IOException {
        WebSocketSession session = openSession();
        doThrow(new IOException("network error")).when(session).sendMessage(any());

        assertThatCode(() -> notificationService.sendToSession(session, "hello")).doesNotThrowAnyException();
    }

    @Test
    void shouldSendDrawOfferToTargetPlayer() throws IOException {
        UUID targetId = UUID.randomUUID();
        WebSocketSession session = openSession();
        when(presenceService.getSessions(targetId)).thenReturn(Set.of(session));

        notificationService.sendDrawOffer(gameId, targetId);

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        assertThat(captor.getValue().getPayload()).contains(gameId.toString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void removeSessionShouldCleanUpSessionLock() {
        WebSocketSession session = openSession();
        notificationService.sendToSession(session, "hello");

        Map<String, ?> locks = (Map<String, ?>) ReflectionTestUtils.getField(notificationService, "sessionLocks");
        assertThat(locks).containsKey(session.getId());

        notificationService.removeSession(session.getId());
        assertThat(locks).doesNotContainKey(session.getId());
    }

    // --- pingAllSessions ---

    @Test
    void pingAllSessionsShouldPingOpenSessions() throws IOException {
        WebSocketSession session = openSession();
        when(presenceService.getAllSessions()).thenReturn(List.of(session));

        notificationService.pingLocalSessions();

        verify(session).sendMessage(any(PingMessage.class));
    }

    @Test
    void pingLocalSessionsShouldRemoveClosedSessions() {
        WebSocketSession session = closedSession();
        UUID userId = UUID.randomUUID();
        when(session.getAttributes()).thenReturn(Map.of("userId", userId));
        when(presenceService.getAllSessions()).thenReturn(List.of(session));

        notificationService.pingLocalSessions();

        verify(presenceService).removeSession(userId, session);
    }

    @Test
    void pingLocalSessionsShouldContinueAfterIoException() throws IOException {
        WebSocketSession failing = openSession();
        WebSocketSession healthy = openSession();
        doThrow(new IOException("ping failed")).when(failing).sendMessage(any(PingMessage.class));
        when(presenceService.getAllSessions()).thenReturn(List.of(failing, healthy));

        assertThatCode(() -> notificationService.pingLocalSessions()).doesNotThrowAnyException();

        verify(healthy).sendMessage(any(PingMessage.class));
    }

    // --- Challenge notifications ---

    @Test
    void onChallengeShouldSendToBothChallengerAndChallenged() throws IOException {
        UUID challengerId = UUID.randomUUID();
        UUID challengedId = UUID.randomUUID();
        WebSocketSession challengerSession = openSession();
        WebSocketSession challengedSession = openSession();
        when(presenceService.getSessions(challengerId)).thenReturn(Set.of(challengerSession));
        when(presenceService.getSessions(challengedId)).thenReturn(Set.of(challengedSession));

        notificationService.onChallenge(challengerId, challengedId, new WsOutgoingChallengeDTO(UUID.randomUUID(), "challenger", "Blitz 5+0"));

        verify(challengerSession).sendMessage(any(TextMessage.class));
        verify(challengedSession).sendMessage(any(TextMessage.class));
    }

    @Test
    void onChallengeCancelledShouldSendToChallenged() throws IOException {
        UUID challengedId = UUID.randomUUID();
        WebSocketSession session = openSession();
        when(presenceService.getSessions(challengedId)).thenReturn(Set.of(session));

        notificationService.onChallengeCancelled(challengedId, new WsOutgoingChallengeCancelledDTO(UUID.randomUUID(), "challenger"));

        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    void onChallengeDeclinedShouldSendToChallenger() throws IOException {
        UUID challengerId = UUID.randomUUID();
        WebSocketSession session = openSession();
        when(presenceService.getSessions(challengerId)).thenReturn(Set.of(session));

        notificationService.onChallengeDeclined(challengerId, new WsOutgoingChallengeResponseDTO(UUID.randomUUID(), "challenged"));

        verify(session).sendMessage(any(TextMessage.class));
    }

    @Test
    void onChallengeExpiredShouldSendToChallenger() throws IOException {
        UUID challengerId = UUID.randomUUID();
        WebSocketSession session = openSession();
        when(presenceService.getSessions(challengerId)).thenReturn(Set.of(session));

        notificationService.onChallengeExpired(challengerId);

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        assertThat(captor.getValue().getPayload()).contains("CHALLENGE_EXPIRED");
    }

    @Test
    void matchStartedPayloadShouldContainGameId() throws IOException {
        WebSocketSession ws = openSession();
        when(presenceService.getSessions(whiteId)).thenReturn(Set.of(ws));
        when(presenceService.getSessions(blackId)).thenReturn(Set.of());

        notificationService.onMatchStarted(new MatchStartedEvent(gameId, whiteId, "white", blackId, "black", 100, 100));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(ws).sendMessage(captor.capture());
        assertThat(captor.getValue().getPayload()).contains(gameId.toString());
    }
}
