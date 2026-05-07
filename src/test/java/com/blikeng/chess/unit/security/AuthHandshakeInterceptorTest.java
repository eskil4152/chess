package com.blikeng.chess.unit.security;

import com.blikeng.chess.security.AuthHandshakeInterceptor;
import com.blikeng.chess.security.JwtPrincipal;
import com.blikeng.chess.security.JwtService;
import com.blikeng.chess.security.UserRole;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthHandshakeInterceptorTest {

    @Mock JwtService jwtService;
    @InjectMocks AuthHandshakeInterceptor interceptor;

    private ServerHttpResponse response() {
        return new ServletServerHttpResponse(new MockHttpServletResponse());
    }

    private WebSocketHandler handler() {
        return mock(WebSocketHandler.class);
    }

    // --- beforeHandshake ---

    @Test
    void beforeHandshakeShouldReturnFalseForNonServletRequest() {
        ServerHttpRequest nonServletRequest = mock(ServerHttpRequest.class);
        assertThat(interceptor.beforeHandshake(nonServletRequest, response(), handler(), new HashMap<>())).isFalse();
    }

    @Test
    void beforeHandshakeShouldReturnFalseWhenNoCookies() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThat(interceptor.beforeHandshake(new ServletServerHttpRequest(request), response(), handler(), new HashMap<>())).isFalse();
    }

    @Test
    void beforeHandshakeShouldReturnFalseWhenNoAuthCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("SESSION", "abc"));
        assertThat(interceptor.beforeHandshake(new ServletServerHttpRequest(request), response(), handler(), new HashMap<>())).isFalse();
    }

    @Test
    void beforeHandshakeShouldReturnFalseWhenTokenIsInvalid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("AUTH", "bad-token"));
        when(jwtService.validateToken("bad-token")).thenReturn(null);
        assertThat(interceptor.beforeHandshake(new ServletServerHttpRequest(request), response(), handler(), new HashMap<>())).isFalse();
    }

    @Test
    void beforeHandshakeShouldSetAttributesAndReturnTrueOnValidToken() {
        UUID userId = UUID.randomUUID();
        JwtPrincipal principal = new JwtPrincipal(userId, "user", UserRole.USER);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("AUTH", "valid-token"));
        when(jwtService.validateToken("valid-token")).thenReturn(principal);

        Map<String, Object> attributes = new HashMap<>();
        boolean result = interceptor.beforeHandshake(new ServletServerHttpRequest(request), response(), handler(), attributes);

        assertThat(result).isTrue();
        assertThat(attributes.get("username")).isEqualTo("user");
        assertThat(attributes.get("userId")).isEqualTo(userId);
    }

    // --- afterHandshake ---

    @Test
    void afterHandshakeShouldDoNothingWhenNoException() {
        interceptor.afterHandshake(mock(ServerHttpRequest.class), response(), handler(), null);
    }

    @Test
    void afterHandshakeShouldNotThrowWhenExceptionPresent() {
        interceptor.afterHandshake(mock(ServerHttpRequest.class), response(), handler(), new RuntimeException("fail"));
    }
}
