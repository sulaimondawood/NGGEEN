package com.dawood.nggeen.shared.infrastructure.security.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean tryConsume(String key, long cap, Duration window) {
        Bucket bucket = buckets.get(key);
        if (bucket == null) {
            bucket = buckets.computeIfAbsent(key, k -> createBucket(cap, window));
        }
        return bucket.tryConsume(1);
    }

    private Bucket createBucket(long cap, Duration window) {
        Bandwidth bandwidth = Bandwidth.builder()
                .capacity(cap)
                .refillIntervally(cap, window)
                .build();
        return Bucket.builder()
                .addLimit(bandwidth)
                .build();
    }
};
