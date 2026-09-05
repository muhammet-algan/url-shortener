package com.urlshortener.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis-based rate limiter using the sliding window counter algorithm.
 *
 * For each client IP, tracks the number of requests within a configurable
 * time window. If the count exceeds the maximum allowed, subsequent
 * requests are rejected until the window resets.
 *
 * Redis key pattern: "rate:create:{ip}"
 * TTL: window duration (auto-cleanup)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;

    @Value("${app.rate-limit.max-requests:10}")
    private int maxRequests;

    @Value("${app.rate-limit.window-seconds:60}")
    private int windowSeconds;

    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled;

    private static final String RATE_LIMIT_PREFIX = "rate:create:";

    /**
     * Check if the given IP has exceeded the rate limit.
     *
     * @param clientIp the client's IP address
     * @return true if the request is allowed, false if rate limit exceeded
     */
    public boolean isAllowed(String clientIp) {
        if (!enabled) {
            return true;
        }

        String key = RATE_LIMIT_PREFIX + clientIp;

        try {
            Long currentCount = redisTemplate.opsForValue().increment(key);
            if (currentCount == null) {
                return true;
            }

            // Set expiry only on first request in window
            if (currentCount == 1) {
                redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
            }

            boolean allowed = currentCount <= maxRequests;
            if (!allowed) {
                log.warn("Rate limit exceeded for IP: {} ({}/{})", clientIp, currentCount, maxRequests);
            }

            return allowed;
        } catch (Exception e) {
            // If Redis is down, fail open (allow the request)
            log.error("Redis error during rate limit check, allowing request: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Get the number of seconds until the rate limit window resets.
     *
     * @param clientIp the client's IP address
     * @return seconds until reset, or 0 if no active window
     */
    public long getRetryAfterSeconds(String clientIp) {
        String key = RATE_LIMIT_PREFIX + clientIp;
        try {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return (ttl != null && ttl > 0) ? ttl : windowSeconds;
        } catch (Exception e) {
            return windowSeconds;
        }
    }

    /**
     * Get remaining requests for the given IP in the current window.
     *
     * @param clientIp the client's IP address
     * @return remaining request count
     */
    public int getRemainingRequests(String clientIp) {
        String key = RATE_LIMIT_PREFIX + clientIp;
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return maxRequests;
            }
            int used = Integer.parseInt(value);
            return Math.max(0, maxRequests - used);
        } catch (Exception e) {
            return maxRequests;
        }
    }
}
