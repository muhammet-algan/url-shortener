package com.kahin.urlshortener.service.impl;

import com.kahin.urlshortener.dto.*;
import com.kahin.urlshortener.entity.ClickEvent;
import com.kahin.urlshortener.entity.Url;
import com.kahin.urlshortener.exception.*;
import com.kahin.urlshortener.repository.ClickEventRepository;
import com.kahin.urlshortener.repository.UrlRepository;
import com.kahin.urlshortener.service.UrlService;
import com.kahin.urlshortener.util.Base62Encoder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Core implementation of the URL shortening service.
 *
 * Handles URL creation, resolution with Redis caching, click tracking,
 * analytics aggregation, and scheduled TTL cleanup.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UrlServiceImpl implements UrlService {

    private final UrlRepository urlRepository;
    private final ClickEventRepository clickEventRepository;
    private final StringRedisTemplate redisTemplate;
    private final Base62Encoder base62Encoder;

    @Value("${app.base-url:http://localhost}")
    private String baseUrl;

    @Value("${app.short-code-length:6}")
    private int shortCodeLength;

    @Value("${app.default-ttl-hours:168}")
    private long defaultTtlHours;

    private static final String CACHE_PREFIX = "url:";
    private static final int MAX_CODE_GENERATION_ATTEMPTS = 10;
    private static final UrlValidator URL_VALIDATOR = new UrlValidator(new String[]{"http", "https"});

    // ════════════════════════════════════════════════════════════════
    //  CREATE
    // ════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public UrlResponse createShortUrl(CreateUrlRequest request) {
        // 1. Validate URL
        if (!URL_VALIDATOR.isValid(request.getOriginalUrl())) {
            throw new InvalidUrlException(request.getOriginalUrl());
        }

        // 2. Generate or validate short code
        String shortCode;
        if (request.getCustomCode() != null && !request.getCustomCode().isBlank()) {
            shortCode = request.getCustomCode().trim();
            if (!base62Encoder.isValidBase62(shortCode)) {
                throw new InvalidUrlException("Custom code must contain only alphanumeric characters");
            }
            if (urlRepository.existsByShortCode(shortCode)) {
                throw new ShortCodeAlreadyExistsException(shortCode);
            }
        } else {
            shortCode = generateUniqueCode();
        }

        // 3. Calculate expiration
        LocalDateTime expiresAt = calculateExpiration(request);

        // 4. Create and save entity
        Url url = Url.builder()
                .shortCode(shortCode)
                .originalUrl(request.getOriginalUrl())
                .expiresAt(expiresAt)
                .build();

        url = urlRepository.save(url);
        log.info("Created short URL: {} → {}", shortCode, request.getOriginalUrl());

        // 5. Cache in Redis
        cacheUrl(shortCode, request.getOriginalUrl(), expiresAt);

        // 6. Build response
        return buildUrlResponse(url);
    }

    // ════════════════════════════════════════════════════════════════
    //  RESOLVE & TRACK
    // ════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public String resolveAndTrack(String shortCode, HttpServletRequest httpRequest) {
        // 1. Try Redis cache first
        String cachedUrl = getCachedUrl(shortCode);
        if (cachedUrl != null) {
            log.debug("Cache HIT for: {}", shortCode);
            // Async: record analytics and increment counter
            recordClickAsync(shortCode, httpRequest);
            return cachedUrl;
        }

        log.debug("Cache MISS for: {}", shortCode);

        // 2. Fallback to database
        Url url = urlRepository.findByShortCodeAndActiveTrue(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        // 3. Check expiration
        if (url.isExpired()) {
            url.setActive(false);
            urlRepository.save(url);
            evictCache(shortCode);
            throw new UrlExpiredException(shortCode);
        }

        // 4. Re-cache for future requests
        cacheUrl(shortCode, url.getOriginalUrl(), url.getExpiresAt());

        // 5. Record analytics asynchronously
        recordClickAsync(shortCode, httpRequest);

        return url.getOriginalUrl();
    }

    // ════════════════════════════════════════════════════════════════
    //  ANALYTICS
    // ════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        // Recent clicks (last 50)
        List<ClickEvent> recentEvents = clickEventRepository
                .findByUrlOrderByClickedAtDesc(url, PageRequest.of(0, 50));

        List<ClickDetail> recentClicks = recentEvents.stream()
                .map(e -> ClickDetail.builder()
                        .clickedAt(e.getClickedAt())
                        .ipAddress(e.getIpAddress())
                        .userAgent(e.getUserAgent())
                        .referer(e.getReferer())
                        .build())
                .collect(Collectors.toList());

        // Unique visitors
        long uniqueVisitors = clickEventRepository.countUniqueVisitors(url);

        // Top referers
        List<Object[]> refererData = clickEventRepository
                .getTopReferers(url, PageRequest.of(0, 10));
        Map<String, Long> topReferers = new LinkedHashMap<>();
        for (Object[] row : refererData) {
            topReferers.put((String) row[0], (Long) row[1]);
        }

        return AnalyticsResponse.builder()
                .shortCode(url.getShortCode())
                .shortUrl(baseUrl + "/" + url.getShortCode())
                .originalUrl(url.getOriginalUrl())
                .totalClicks(url.getClickCount())
                .uniqueVisitors(uniqueVisitors)
                .createdAt(url.getCreatedAt())
                .lastAccessedAt(url.getLastAccessedAt())
                .expiresAt(url.getExpiresAt())
                .active(url.isActive())
                .recentClicks(recentClicks)
                .topReferers(topReferers)
                .build();
    }

    // ════════════════════════════════════════════════════════════════
    //  DELETE
    // ════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public void deleteUrl(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));
        url.setActive(false);
        urlRepository.save(url);
        evictCache(shortCode);
        log.info("Deactivated URL: {}", shortCode);
    }

    // ════════════════════════════════════════════════════════════════
    //  SCHEDULED TASKS
    // ════════════════════════════════════════════════════════════════

    /**
     * Periodically deactivate expired URLs.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedRate = 300_000)
    @Transactional
    public void cleanupExpiredUrls() {
        int count = urlRepository.deactivateExpiredUrls(LocalDateTime.now());
        if (count > 0) {
            log.info("Cleaned up {} expired URLs", count);
        }
    }

    // ════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ════════════════════════════════════════════════════════════════

    /**
     * Generate a unique short code, retrying up to MAX_CODE_GENERATION_ATTEMPTS times.
     */
    private String generateUniqueCode() {
        for (int i = 0; i < MAX_CODE_GENERATION_ATTEMPTS; i++) {
            String code = base62Encoder.generateCode(shortCodeLength);
            if (!urlRepository.existsByShortCode(code)) {
                return code;
            }
            log.debug("Short code collision on attempt {}: {}", i + 1, code);
        }
        // Extremely unlikely with 62^6 combinations, but handle gracefully
        throw new RuntimeException("Failed to generate unique short code after "
                + MAX_CODE_GENERATION_ATTEMPTS + " attempts");
    }

    /**
     * Calculate expiration time from the request parameters.
     * Priority: expiresAt > ttlHours > default TTL
     */
    private LocalDateTime calculateExpiration(CreateUrlRequest request) {
        if (request.getExpiresAt() != null) {
            return request.getExpiresAt();
        }
        long hours = (request.getTtlHours() != null) ? request.getTtlHours() : defaultTtlHours;
        return LocalDateTime.now().plusHours(hours);
    }

    /**
     * Cache the URL mapping in Redis with TTL matching the URL's expiration.
     */
    private void cacheUrl(String shortCode, String originalUrl, LocalDateTime expiresAt) {
        try {
            String key = CACHE_PREFIX + shortCode;
            if (expiresAt != null) {
                long ttlSeconds = Duration.between(LocalDateTime.now(), expiresAt).getSeconds();
                if (ttlSeconds > 0) {
                    redisTemplate.opsForValue().set(key, originalUrl, ttlSeconds, TimeUnit.SECONDS);
                }
            } else {
                redisTemplate.opsForValue().set(key, originalUrl);
            }
        } catch (Exception e) {
            log.warn("Failed to cache URL {}: {}", shortCode, e.getMessage());
        }
    }

    /**
     * Try to get URL from Redis cache.
     */
    private String getCachedUrl(String shortCode) {
        try {
            return redisTemplate.opsForValue().get(CACHE_PREFIX + shortCode);
        } catch (Exception e) {
            log.warn("Redis read error for {}: {}", shortCode, e.getMessage());
            return null;
        }
    }

    /**
     * Remove URL from Redis cache.
     */
    private void evictCache(String shortCode) {
        try {
            redisTemplate.delete(CACHE_PREFIX + shortCode);
        } catch (Exception e) {
            log.warn("Failed to evict cache for {}: {}", shortCode, e.getMessage());
        }
    }

    /**
     * Record a click event asynchronously to avoid blocking the redirect response.
     */
    @Async
    @Transactional
    public void recordClickAsync(String shortCode, HttpServletRequest request) {
        try {
            // Increment click count atomically
            urlRepository.incrementClickCount(shortCode, LocalDateTime.now());

            // Find the URL for the foreign key
            Url url = urlRepository.findByShortCode(shortCode).orElse(null);
            if (url == null) return;

            // Save detailed click event
            ClickEvent event = ClickEvent.builder()
                    .url(url)
                    .ipAddress(extractClientIp(request))
                    .userAgent(truncate(request.getHeader("User-Agent"), 512))
                    .referer(truncate(request.getHeader("Referer"), 2048))
                    .build();
            clickEventRepository.save(event);

            log.debug("Recorded click for: {} from IP: {}", shortCode, event.getIpAddress());
        } catch (Exception e) {
            log.error("Failed to record click for {}: {}", shortCode, e.getMessage());
        }
    }

    /**
     * Extract real client IP, handling X-Forwarded-For from Nginx proxy.
     */
    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    /**
     * Build the standard URL response DTO.
     */
    private UrlResponse buildUrlResponse(Url url) {
        return UrlResponse.builder()
                .shortCode(url.getShortCode())
                .shortUrl(baseUrl + "/" + url.getShortCode())
                .originalUrl(url.getOriginalUrl())
                .createdAt(url.getCreatedAt())
                .expiresAt(url.getExpiresAt())
                .clickCount(url.getClickCount())
                .build();
    }

    /**
     * Safely truncate a string to maxLength characters.
     */
    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
