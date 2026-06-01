package com.blikeng.chess.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;

// TODO: Replace with a proper blacklist. Maybe redis with configured persistence

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
        if (token == null) return false;
        return blacklist.getIfPresent(token) != null;
    }
}
