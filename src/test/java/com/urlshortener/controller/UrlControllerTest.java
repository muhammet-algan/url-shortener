package com.urlshortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.dto.AnalyticsResponse;
import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.exception.GlobalExceptionHandler;
import com.urlshortener.exception.RateLimitExceededException;
import com.urlshortener.service.RateLimiterService;
import com.urlshortener.service.UrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UrlController Unit Tests")
class UrlControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UrlService urlService;

    @Mock
    private RateLimiterService rateLimiterService;

    @InjectMocks
    private UrlController urlController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(urlController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/urls should return 201 Created when request is valid")
    void testCreateUrlSuccess() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://example.com/target");

        UrlResponse response = UrlResponse.builder()
                .shortCode("abc123")
                .shortUrl("http://localhost/abc123")
                .originalUrl("https://example.com/target")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .clickCount(0L)
                .build();

        when(rateLimiterService.isAllowed(anyString())).thenReturn(true);
        when(rateLimiterService.getRemainingRequests(anyString())).thenReturn(9);
        when(urlService.createShortUrl(any(CreateUrlRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-RateLimit-Remaining", "9"))
                .andExpect(jsonPath("$.shortCode").value("abc123"))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com/target"));
    }

    @Test
    @DisplayName("POST /api/v1/urls should return 429 when rate limit is exceeded")
    void testCreateUrlRateLimitExceeded() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://example.com/target");

        when(rateLimiterService.isAllowed(anyString())).thenReturn(false);
        when(rateLimiterService.getRetryAfterSeconds(anyString())).thenReturn(45L);

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "45"));
    }

    @Test
    @DisplayName("GET /{shortCode} should redirect 302 Found to original URL")
    void testRedirectSuccess() throws Exception {
        when(urlService.getOriginalUrl(eq("abc123"), any())).thenReturn("https://example.com/target");

        mockMvc.perform(get("/abc123"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/target"));
    }

    @Test
    @DisplayName("GET /api/v1/urls/{shortCode}/stats should return 200 OK with analytics data")
    void testGetStatsSuccess() throws Exception {
        AnalyticsResponse analytics = AnalyticsResponse.builder()
                .shortCode("abc123")
                .shortUrl("http://localhost/abc123")
                .originalUrl("https://example.com/target")
                .totalClicks(42L)
                .uniqueVisitors(30L)
                .createdAt(LocalDateTime.now().minusDays(1))
                .active(true)
                .recentClicks(List.of())
                .topReferers(Map.of("direct", 42L))
                .build();

        when(urlService.getAnalytics("abc123")).thenReturn(analytics);

        mockMvc.perform(get("/api/v1/urls/abc123/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClicks").value(42))
                .andExpect(jsonPath("$.uniqueVisitors").value(30));
    }

    @Test
    @DisplayName("GET /api/v1/health should return 200 OK and UP status")
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("url-shortener"));
    }

    @Test
    @DisplayName("DELETE /api/v1/urls/{shortCode} should return 200 OK")
    void testDeleteUrl() throws Exception {
        mockMvc.perform(delete("/api/v1/urls/abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEACTIVATED"));
    }
}
