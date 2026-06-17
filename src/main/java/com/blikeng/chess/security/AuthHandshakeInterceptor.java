package com.blikeng.chess.security;

import jakarta.servlet.http.Cookie;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Authenticates the WebSocket handshake: reads the {@code AUTH} cookie, validates the JWT
 * via {@link JwtService}, and on success stashes {@code userId}/{@code username} in the
 * session attributes. Rejects the handshake (returns false) if the cookie is missing or
 * the token is invalid.
 */
@Component
public class AuthHandshakeInterceptor implements HandshakeInterceptor {
    private final Logger logger = LoggerFactory.getLogger(AuthHandshakeInterceptor.class);

    private final JwtService jwtService;

    public AuthHandshakeInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest)) return false;

        Cookie[] cookieList = ((ServletServerHttpRequest) request).getServletRequest().getCookies();
        if (cookieList == null) return false;

        String token = null;
        for (Cookie cookie : cookieList) {
            if (cookie.getName().equals("AUTH")) {
                token = cookie.getValue();
                break;
            }
        }

        if (token == null) return false;

        JwtPrincipal principal = jwtService.validateToken(token);
        if (principal == null) return false;

        attributes.put("username", principal.username());
        attributes.put("userId", principal.userId());

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, @Nullable Exception exception) {
        if (exception != null) logger.error("Handshake failed: {}", exception.getMessage());
    }
}
