package com.blikeng.chess.security.ratelimit;

import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimitingService {
    public boolean tryConsume(String key, Long maxTokens, Duration window){};
}
