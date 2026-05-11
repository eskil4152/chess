package com.blikeng.chess.security.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import com.github.benmanes.caffeine.cache.Caffeine;


@Service
public class RateLimitingService {
    private final Cache<String, Bucket> buckets =
            Caffeine
                    .newBuilder()
                    .expireAfterAccess(Duration.ofMinutes(10))
                    .build();

    public boolean tryConsume(String key, Long maxTokens, Duration window){
        Bucket bucket = buckets.get(key, _ ->
            Bucket
                    .builder()
                    .addLimit(
                            Bandwidth
                                    .builder()
                                    .capacity(maxTokens)
                                    .refillGreedy(maxTokens, window)
                                    .build()
                    ).build());

        return bucket.tryConsume(1);
    }
}
