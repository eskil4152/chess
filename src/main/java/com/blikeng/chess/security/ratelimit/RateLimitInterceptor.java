package com.blikeng.chess.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * Per-IP rate limiter for {@code /api/**} (registered in {@code RateLimitConfig}).
 *
 * <p>Buckets are keyed by client IP and path category: login, register, and everything
 * else - each with its own per-minute token limit (configured under {@code rate-limit.*} in {@code application.yaml}).
 * Requests over the limit get HTTP 429. All clients behind one IP share a bucket.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    @Value("${rate-limit.login.max-tokens}") private Long maxLoginTokens;
    @Value("${rate-limit.register.max-tokens}") private Long registerMaxTokens;
    @Value("${rate-limit.other.max-tokens}") private Long otherMaxTokens;

    private static final Logger logger = LoggerFactory.getLogger(RateLimitInterceptor.class);
    private final RateLimitingService rateLimitingService;

    public RateLimitInterceptor(RateLimitingService rateLimitingService) {
        this.rateLimitingService = rateLimitingService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = request.getRemoteAddr();
        if (ip == null) ip = "unknown";

        String path = request.getRequestURI();

        boolean allowed = switch (path) {
            case "/api/auth/login" -> rateLimitingService.tryConsume("login:"+ip, maxLoginTokens, Duration.ofMinutes(1));

            case "/api/auth/register" -> rateLimitingService.tryConsume("register:"+ip, registerMaxTokens, Duration.ofMinutes(1));

            default -> rateLimitingService.tryConsume("others:"+ip, otherMaxTokens, Duration.ofMinutes(1));
        };

        if (!allowed) {
            logger.warn("Rate limit exceeded for IP: {} at path: {}", ip, path);

            response.sendError(429, "Rate limit exceeded");
            return false;
        }

        return true;
    }
}
