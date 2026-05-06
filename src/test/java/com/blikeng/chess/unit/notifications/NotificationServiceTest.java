package com.blikeng.chess.unit.notifications;

import com.blikeng.chess.model.EndedBy;
import com.blikeng.chess.model.GameStatus;
import com.blikeng.chess.notifications.events.MatchEndedEvent;
import com.blikeng.chess.notifications.events.MatchStartedEvent;
import com.blikeng.chess.notifications.events.MoveMadeEvent;
import com.blikeng.chess.service.PresenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.blikeng.chess.notifications.NotificationService;

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

    private WebSocketSession openSession() throws IOException {
        WebSocketSession s = mock(WebSocketSession.class);
        when(s.isOpen()).thenReturn(true);
        return s;
    }

    @Test
    void shouldSendMatchStartedToBothPlayers() throws IOException {
        WebSocketSession ws = openSession();
        WebSocketSession bs = openSession();
        when(presenceService.getSessions(whiteId)).thenReturn(Set.of(ws));
        when(presenceService.getSessions(blackId)).thenReturn(Set.of(bs));

        notificationService.onMatchStarted(new MatchStartedEvent(gameId, whiteId, "white", blackId, "black"));

        verify(ws).sendMessage(any(TextMessage.class));
        verify(bs).sendMessage(any(TextMessage.class));
    }

    @Test
    void shouldSendMoveMadeToBothPlayers() throws IOException {
        WebSocketSession ws = openSession();
        WebSocketSession bs = openSession();
        when(presenceService.getSessions(whiteId)).thenReturn(Set.of(ws));
        when(presenceService.getSessions(blackId)).thenReturn(Set.of(bs));

        notificationService.onMoveMade(new MoveMadeEvent(gameId, whiteId, blackId, "e2e4"));

        verify(ws).sendMessage(any(TextMessage.class));
        verify(bs).sendMessage(any(TextMessage.class));
    }

    @Test
    void shouldSendMatchEndedToBothPlayers() throws IOException {
        WebSocketSession ws = openSession();
        WebSocketSession bs = openSession();
        when(presenceService.getSessions(whiteId)).thenReturn(Set.of(ws));
        when(presenceService.getSessions(blackId)).thenReturn(Set.of(bs));

        notificationService.onMatchEnded(new MatchEndedEvent(gameId, whiteId, blackId, GameStatus.WHITE_WIN, EndedBy.CHECKMATE));

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
        WebSocketSession closed = mock(WebSocketSession.class);
        when(closed.isOpen()).thenReturn(false);
        notificationService.sendToSession(closed, "hello");
        verify(closed, never()).sendMessage(any());
    }

    @Test
    void ioExceptionShouldNotPropagate() throws IOException {
        WebSocketSession session = openSession();
        doThrow(new IOException("network error")).when(session).sendMessage(any());

        notificationService.sendToSession(session, "hello");
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
    void matchStartedPayloadShouldContainGameId() throws IOException {
        WebSocketSession ws = openSession();
        when(presenceService.getSessions(whiteId)).thenReturn(Set.of(ws));
        when(presenceService.getSessions(blackId)).thenReturn(Set.of());

        notificationService.onMatchStarted(new MatchStartedEvent(gameId, whiteId, "white", blackId, "black"));

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(ws).sendMessage(captor.capture());
        assertThat(captor.getValue().getPayload()).contains(gameId.toString());
    }
}
