package com.urlshortener.controller;

import com.urlshortener.dto.AnalyticsResponse;
import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.exception.RateLimitExceededException;
import com.urlshortener.service.QrCodeService;
import com.urlshortener.service.RateLimiterService;
import com.urlshortener.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

/**
 * REST controller handling all URL shortener endpoints.
 *
 * Endpoints:
 *   POST   /api/v1/urls              → Create short URL
 *   GET    /{shortCode}              → Redirect to original URL
 *   GET    /api/v1/urls/{code}/stats → Get analytics
 *   GET    /api/v1/urls/{code}/qr    → Get QR Code image (PNG)
 *   DELETE /api/v1/urls/{code}       → Deactivate URL
 *   GET    /api/v1/health            → Health check
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class UrlController {

    private final UrlService urlService;
    private final RateLimiterService rateLimiterService;
    private final QrCodeService qrCodeService;

    // ════════════════════════════════════════════════════════════════
    //  CREATE SHORT URL
    // ════════════════════════════════════════════════════════════════

    /**
     * Create a new shortened URL.
     * Rate limited: 10 requests per minute per IP.
     */
    @PostMapping("/api/v1/urls")
    public ResponseEntity<UrlResponse> createUrl(
            @Valid @RequestBody CreateUrlRequest request,
            HttpServletRequest httpRequest) {

        // Rate limit check
        String clientIp = extractClientIp(httpRequest);
        if (!rateLimiterService.isAllowed(clientIp)) {
            long retryAfter = rateLimiterService.getRetryAfterSeconds(clientIp);
            throw new RateLimitExceededException(retryAfter);
        }

        UrlResponse response = urlService.createShortUrl(request);

        // Include rate limit headers in response
        int remaining = rateLimiterService.getRemainingRequests(clientIp);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("X-RateLimit-Remaining", String.valueOf(remaining))
                .header("X-RateLimit-Limit", "10")
                .body(response);
    }

    // ════════════════════════════════════════════════════════════════
    //  REDIRECT
    // ════════════════════════════════════════════════════════════════

    /**
     * Redirect to the original URL.
     * Regex constraint ensures only valid Base62 codes (3-12 alphanumeric chars) are matched,
     * preventing this handler from intercepting static file requests like index.html.
     * Uses HTTP 302 (Found) for proper redirect behavior.
     */
    @GetMapping("/{shortCode:[a-zA-Z0-9]{3,12}}")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode,
            HttpServletRequest httpRequest) {

        String originalUrl = urlService.resolveAndTrack(shortCode, httpRequest);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(originalUrl));

        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    // ════════════════════════════════════════════════════════════════
    //  ANALYTICS
    // ════════════════════════════════════════════════════════════════

    /**
     * Get comprehensive analytics for a shortened URL.
     */
    @GetMapping("/api/v1/urls/{shortCode}/stats")
    public ResponseEntity<AnalyticsResponse> getStats(@PathVariable String shortCode) {
        AnalyticsResponse analytics = urlService.getAnalytics(shortCode);
        return ResponseEntity.ok(analytics);
    }

    // ════════════════════════════════════════════════════════════════
    //  QR CODE
    // ════════════════════════════════════════════════════════════════

    /**
     * Generate and return a QR code PNG image for the shortened URL.
     */
    @GetMapping(value = "/api/v1/urls/{shortCode}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQrCode(
            @PathVariable String shortCode,
            @RequestParam(defaultValue = "300") int size,
            HttpServletRequest request) {
        urlService.getOriginalUrl(shortCode, null); // validates existence and expiration
        String host = request.getHeader("Host");
        String scheme = request.getHeader("X-Forwarded-Proto") != null ? request.getHeader("X-Forwarded-Proto") : request.getScheme();
        String targetShortUrl = scheme + "://" + host + "/" + shortCode;
        byte[] qrBytes = qrCodeService.generateQrCode(targetShortUrl, size, size);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"qr-" + shortCode + ".png\"")
                .contentType(MediaType.IMAGE_PNG)
                .body(qrBytes);
    }

    // ════════════════════════════════════════════════════════════════
    //  DELETE
    // ════════════════════════════════════════════════════════════════

    /**
     * Deactivate (soft-delete) a shortened URL.
     */
    @DeleteMapping("/api/v1/urls/{shortCode}")
    public ResponseEntity<Map<String, String>> deleteUrl(@PathVariable String shortCode) {
        urlService.deleteUrl(shortCode);
        return ResponseEntity.ok(Map.of(
                "message", "URL deactivated successfully",
                "shortCode", shortCode
        ));
    }

    // ════════════════════════════════════════════════════════════════
    //  HEALTH CHECK
    // ════════════════════════════════════════════════════════════════

    /**
     * Health check endpoint for Nginx load balancer.
     */
    @GetMapping("/api/v1/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "url-shortener",
                "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }

    // ── Helper ──

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
}
