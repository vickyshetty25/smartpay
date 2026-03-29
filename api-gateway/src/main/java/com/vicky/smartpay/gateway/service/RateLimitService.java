package com.vicky.smartpay.gateway.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;

    // Max 10 requests per user per 60 seconds
    private static final int MAX_REQUESTS = 10;
    private static final int WINDOW_SECONDS = 60;

    public RateLimitService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(Long userId) {
        String key = "ratelimit:smartpay:" + userId;
        long now = Instant.now().getEpochSecond();
        long windowStart = now - WINDOW_SECONDS;

        // Remove old timestamps outside the window
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

        // Count requests in current window
        Long count = redisTemplate.opsForZSet().zCard(key);

        if (count != null && count >= MAX_REQUESTS) {
            return false;
        }

        // Add current request timestamp
        redisTemplate.opsForZSet().add(key, String.valueOf(now), now);
        redisTemplate.expire(key, WINDOW_SECONDS + 10, TimeUnit.SECONDS);

        return true;
    }
}