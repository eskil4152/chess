package com.blikeng.chess.config;

import com.blikeng.chess.security.ratelimit.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers {@link RateLimitInterceptor} on all {@code /api/**} endpoints.
 *
 * <p>Throttling is per client IP, the limits live under {@code rate-limit.*} in
 * application.yaml.
 */
@Configuration
public class RateLimitConfig implements WebMvcConfigurer {
    private final RateLimitInterceptor rateLimitInterceptor;

    public RateLimitConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry
                .addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**");
    }
}
