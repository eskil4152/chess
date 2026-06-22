package com.blikeng.chess.unit.service;

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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock PresenceService presenceService;
    @Mock RedisTemplate<String, String> redisTemplate;
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
    void shouldSendMatchStartedToBothPlayers() {
        notificationService.onMatchStarted(new MatchStartedEvent(gameId, whiteId, "white", blackId, "black", 100, 100));

        verify(redisTemplate).convertAndSend(eq("user:" + whiteId), anyString());
        verify(redisTemplate).convertAndSend(eq("user:" + blackId), anyString());
    }

    @Test
    void shouldSendMoveMadeToBothPlayers() {
        notificationService.onMoveMade(new MoveMadeEvent(gameId, whiteId, blackId, "e2e4", true, 0, Set.of()));

        verify(redisTemplate).convertAndSend(eq("user:" + whiteId), anyString());
        verify(redisTemplate).convertAndSend(eq("user:" + blackId), anyString());
    }

    @Test
    void shouldSendMatchEndedToBothPlayers() {
        notificationService.onMatchEnded(new MatchEndedEvent(gameId, whiteId, blackId, GameStatus.WHITE_WIN, EndedBy.CHECKMATE, 100, 100, Set.of()));

        verify(redisTemplate).convertAndSend(eq("user:" + whiteId), anyString());
        verify(redisTemplate).convertAndSend(eq("user:" + blackId), anyString());
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
    void shouldSendDrawOfferToTargetPlayer() {
        UUID targetId = UUID.randomUUID();

        notificationService.sendDrawOffer(gameId, targetId);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(eq("user:" + targetId), captor.capture());
        assertThat(captor.getValue()).contains(gameId.toString());
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
    void onChallengeShouldSendToBothChallengerAndChallenged() {
        UUID challengerId = UUID.randomUUID();
        UUID challengedId = UUID.randomUUID();

        notificationService.onChallenge(challengerId, challengedId, new WsOutgoingChallengeDTO(UUID.randomUUID(), "challenger", "Blitz 5+0"));

        verify(redisTemplate).convertAndSend(eq("user:" + challengerId), anyString());
        verify(redisTemplate).convertAndSend(eq("user:" + challengedId), anyString());
    }

    @Test
    void onChallengeCancelledShouldSendToChallenged() {
        UUID challengedId = UUID.randomUUID();

        notificationService.onChallengeCancelled(challengedId, new WsOutgoingChallengeCancelledDTO(UUID.randomUUID(), "challenger"));

        verify(redisTemplate).convertAndSend(eq("user:" + challengedId), anyString());
    }

    @Test
    void onChallengeDeclinedShouldSendToChallenger() {
        UUID challengerId = UUID.randomUUID();

        notificationService.onChallengeDeclined(challengerId, new WsOutgoingChallengeResponseDTO(UUID.randomUUID(), "challenged"));

        verify(redisTemplate).convertAndSend(eq("user:" + challengerId), anyString());
    }

    @Test
    void onChallengeExpiredShouldSendToChallenger() {
        UUID challengerId = UUID.randomUUID();

        notificationService.onChallengeExpired(challengerId);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(eq("user:" + challengerId), captor.capture());
        assertThat(captor.getValue()).contains("CHALLENGE_EXPIRED");
    }

    @Test
    void matchStartedPayloadShouldContainGameId() {
        notificationService.onMatchStarted(new MatchStartedEvent(gameId, whiteId, "white", blackId, "black", 100, 100));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(eq("user:" + whiteId), captor.capture());
        assertThat(captor.getValue()).contains(gameId.toString());
    }
}
