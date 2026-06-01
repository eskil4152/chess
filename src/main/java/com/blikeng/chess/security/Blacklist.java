package com.blikeng.chess.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class Blacklist {
    private final Cache<String, String> blacklist =
        Caffeine
            .newBuilder()
            .expireAfterWrite(Duration.ofDays(30))
            .build();

    public void add(String token) {
        blacklist.put(token, token);
    }

    public boolean contains(String token) {
        return blacklist.getIfPresent(token) != null;
    }
}
