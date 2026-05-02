package com.blikeng.chess.config;

import com.blikeng.chess.security.AuthHandshakeInterceptor;
import com.blikeng.chess.websocket.WebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final WebSocketHandler webSocketHandler;
    private final AuthHandshakeInterceptor authHandshakeInterceptor;

    public WebSocketConfig(WebSocketHandler webSocketHandler, AuthHandshakeInterceptor authHandshakeInterceptor) {
        this.webSocketHandler = webSocketHandler;
        this.authHandshakeInterceptor = authHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/ws")
                .addInterceptors(authHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
