package com.blikeng.chess.unit.websocket;

import com.blikeng.chess.dto.websocket.WsDrawDTO;
import com.blikeng.chess.dto.websocket.WsMoveDTO;
import com.blikeng.chess.dto.websocket.WsResignDTO;
import com.blikeng.chess.exception.types.InvalidMoveException;
import com.blikeng.chess.notifications.NotificationService;
import com.blikeng.chess.service.GameService;
import com.blikeng.chess.service.MatchmakingService;
import com.blikeng.chess.service.PresenceService;
import com.blikeng.chess.websocket.WebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketHandlerTest {

    @Mock GameService gameService;
    @Mock MatchmakingService matchmakingService;
    @Mock PresenceService presenceService;
    @Mock NotificationService notificationService;
    @InjectMocks WebSocketHandler handler;

    private WebSocketSession session;
    private UUID userId;

    @BeforeEach
    void setup() {
        userId = UUID.randomUUID();
        session = mock(WebSocketSession.class);
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("userId", userId);
        when(session.getAttributes()).thenReturn(attrs);
    }

    // After connection established
    @Test
    void shouldSaveSessionOnConnectionEstablished() {
        handler.afterConnectionEstablished(session);

        verify(presenceService).saveSession(userId, session);
        verifyNoInteractions(gameService, matchmakingService);
    }


    // --- Incoming messages ---
    @Test
    void shouldHandleMessageOfTypeMove() {
        String gameId = UUID.randomUUID().toString();
        String payload = String.format("{\"type\":\"MOVE\",\"gameId\":\"%s\",\"move\":\"e2e4\"}", gameId);

        handler.handleTextMessage(session, new TextMessage(payload));

        verify(gameService).makeMove(eq(userId), any(WsMoveDTO.class));
    }

    @Test
    void shouldHandleMessageOfTypeResign() {
        String gameId = UUID.randomUUID().toString();
        String payload = String.format("{\"type\":\"RESIGN\",\"gameId\":\"%s\"}", gameId);

        handler.handleTextMessage(session, new TextMessage(payload));

        verify(gameService).resignGame(eq(userId), any(WsResignDTO.class));
    }

    @Test
    void shouldHandleMessageOfTypeOfferDraw() {
        String gameId = UUID.randomUUID().toString();
        String payload = String.format("{\"type\":\"OFFER_DRAW\",\"gameId\":\"%s\"}", gameId);

        handler.handleTextMessage(session, new TextMessage(payload));

        verify(gameService).handleDraw(eq(userId), any(WsDrawDTO.class));
    }

    @Test
    void shouldHandleTextMessageOfUnknownTypeByIgnoring() {
        String payload = "{\"type\":\"UNKNOWN\"}";
        handler.handleTextMessage(session, new TextMessage(payload));
        verifyNoInteractions(gameService);
    }

    @Test
    void shouldReceiveWebSocketErrorIfApiExceptionIsThrown() {
        String gameId = UUID.randomUUID().toString();
        String payload = String.format("{\"type\":\"MOVE\",\"gameId\":\"%s\",\"move\":\"e2e4\"}", gameId);
        doThrow(new InvalidMoveException()).when(gameService).makeMove(eq(userId), any());

        handler.handleTextMessage(session, new TextMessage(payload));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).sendToSession(eq(session), captor.capture());
        assertThat(captor.getValue()).contains("ERROR").contains("400");
    }

    @Test
    void shouldReceiveWebSocketErrorIfUnknownErrorIsThrown() {
        String gameId = UUID.randomUUID().toString();
        String payload = String.format("{\"type\":\"MOVE\",\"gameId\":\"%s\",\"move\":\"e2e4\"}", gameId);
        doThrow(new RuntimeException("unexpected")).when(gameService).makeMove(eq(userId), any());

        handler.handleTextMessage(session, new TextMessage(payload));

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).sendToSession(eq(session), captor.capture());
        assertThat(captor.getValue()).contains("500");
    }


    // --- Connection closed ---
    @Test
    void shouldRemoveSessionAndDequeuePlayerWhenDisconnectingLastSession() {
        when(presenceService.hasNoSessions(userId)).thenReturn(true);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(presenceService).removeSession(userId, session);
        verify(matchmakingService).dequeuePlayer(userId);
    }

    @Test
    void shouldRemoveSessionButRemainInQueueWhenMoreSessionsExist() {
        when(presenceService.hasNoSessions(userId)).thenReturn(false);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(presenceService).removeSession(userId, session);
        verify(matchmakingService, never()).dequeuePlayer(any());
    }
}
