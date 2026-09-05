package com.urlshortener.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimiterService Unit Tests")
class RateLimiterServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RateLimiterService rateLimiterService;

    private static final String IP = "192.168.1.100";
    private static final String KEY = "rate:create:192.168.1.100";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(rateLimiterService, "maxRequests", 10);
        ReflectionTestUtils.setField(rateLimiterService, "windowSeconds", 60);
        ReflectionTestUtils.setField(rateLimiterService, "enabled", true);
    }

    @Test
    @DisplayName("Should allow request when rate limiting is disabled")
    void testRateLimitDisabled() {
        ReflectionTestUtils.setField(rateLimiterService, "enabled", false);
        assertTrue(rateLimiterService.isAllowed(IP));
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("First request should set key expiration and allow access")
    void testFirstRequestSetsExpiry() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(KEY)).thenReturn(1L);

        boolean allowed = rateLimiterService.isAllowed(IP);

        assertTrue(allowed);
        verify(redisTemplate).expire(KEY, 60, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("Request under limit should be allowed without resetting expiration")
    void testRequestUnderLimit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(KEY)).thenReturn(5L);

        boolean allowed = rateLimiterService.isAllowed(IP);

        assertTrue(allowed);
        verify(redisTemplate, never()).expire(anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("Request exceeding limit should be rejected")
    void testRequestExceedingLimit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(KEY)).thenReturn(11L);

        boolean allowed = rateLimiterService.isAllowed(IP);

        assertFalse(allowed);
    }

    @Test
    @DisplayName("Fail-open strategy: should allow request if Redis fails")
    void testFailOpenOnRedisError() {
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection lost"));

        boolean allowed = rateLimiterService.isAllowed(IP);

        assertTrue(allowed, "Should fail-open when Redis is unavailable");
    }

    @Test
    @DisplayName("Should return remaining requests correctly")
    void testGetRemainingRequests() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn("4");

        int remaining = rateLimiterService.getRemainingRequests(IP);

        assertEquals(6, remaining);
    }

    @Test
    @DisplayName("Should return remaining requests as 0 when count exceeds max")
    void testGetRemainingRequestsExceeded() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(KEY)).thenReturn("15");

        int remaining = rateLimiterService.getRemainingRequests(IP);

        assertEquals(0, remaining);
    }

    @Test
    @DisplayName("Should return TTL for retry-after")
    void testGetRetryAfterSeconds() {
        when(redisTemplate.getExpire(KEY, TimeUnit.SECONDS)).thenReturn(42L);

        long retryAfter = rateLimiterService.getRetryAfterSeconds(IP);

        assertEquals(42L, retryAfter);
    }
}
