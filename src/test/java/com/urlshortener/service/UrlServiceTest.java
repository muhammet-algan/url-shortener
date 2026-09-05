package com.urlshortener.service;

import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.entity.Url;
import com.urlshortener.exception.InvalidUrlException;
import com.urlshortener.exception.ShortCodeAlreadyExistsException;
import com.urlshortener.exception.UrlExpiredException;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.repository.ClickEventRepository;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.service.impl.UrlServiceImpl;
import com.urlshortener.util.Base62Encoder;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UrlService Unit Tests")
class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private ClickEventRepository clickEventRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private Base62Encoder base62Encoder;

    @InjectMocks
    private UrlServiceImpl urlService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(urlService, "baseUrl", "http://localhost");
        ReflectionTestUtils.setField(urlService, "shortCodeLength", 6);
        ReflectionTestUtils.setField(urlService, "defaultTtlHours", 168L);
    }

    @Test
    @DisplayName("Should successfully create short URL with generated code")
    void testCreateShortUrlSuccess() {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://example.com/very-long-path");

        when(base62Encoder.generateCode(6)).thenReturn("abc123");
        when(urlRepository.existsByShortCode("abc123")).thenReturn(false);

        Url savedUrl = Url.builder()
                .id(1L)
                .shortCode("abc123")
                .originalUrl("https://example.com/very-long-path")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(168))
                .clickCount(0L)
                .active(true)
                .build();

        when(urlRepository.save(any(Url.class))).thenReturn(savedUrl);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        UrlResponse response = urlService.createShortUrl(request);

        assertNotNull(response);
        assertEquals("abc123", response.getShortCode());
        assertEquals("https://example.com/very-long-path", response.getOriginalUrl());
        assertEquals("http://localhost/abc123", response.getShortUrl());
        verify(urlRepository).save(any(Url.class));
    }

    @Test
    @DisplayName("Should throw exception when URL is invalid")
    void testCreateShortUrlInvalidUrl() {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("invalid-not-a-url");

        assertThrows(InvalidUrlException.class, () -> urlService.createShortUrl(request));
        verify(urlRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when custom code is a reserved keyword")
    void testCreateShortUrlReservedWord() {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://example.com");
        request.setCustomCode("api");

        assertThrows(InvalidUrlException.class, () -> urlService.createShortUrl(request));
    }

    @Test
    @DisplayName("Should throw exception when custom code already exists")
    void testCreateShortUrlCodeAlreadyExists() {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://example.com");
        request.setCustomCode("mycode");

        when(base62Encoder.isValidBase62("mycode")).thenReturn(true);
        when(urlRepository.existsByShortCode("mycode")).thenReturn(true);

        assertThrows(ShortCodeAlreadyExistsException.class, () -> urlService.createShortUrl(request));
    }

    @Test
    @DisplayName("Should return original URL from Redis cache hit")
    void testGetOriginalUrlCacheHit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("url:abc123")).thenReturn("https://cached-example.com");

        String result = urlService.getOriginalUrl("abc123", null);

        assertEquals("https://cached-example.com", result);
        verify(urlRepository, never()).findByShortCodeAndActiveTrue(anyString());
    }

    @Test
    @DisplayName("Should query database when Redis cache misses")
    void testGetOriginalUrlCacheMiss() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("url:abc123")).thenReturn(null);

        Url url = Url.builder()
                .id(1L)
                .shortCode("abc123")
                .originalUrl("https://database-example.com")
                .active(true)
                .expiresAt(LocalDateTime.now().plusHours(10))
                .build();

        when(urlRepository.findByShortCodeAndActiveTrue("abc123")).thenReturn(Optional.of(url));

        String result = urlService.getOriginalUrl("abc123", null);

        assertEquals("https://database-example.com", result);
        verify(urlRepository).findByShortCodeAndActiveTrue("abc123");
    }

    @Test
    @DisplayName("Should throw UrlNotFoundException when code does not exist")
    void testGetOriginalUrlNotFound() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("url:unknown")).thenReturn(null);
        when(urlRepository.findByShortCodeAndActiveTrue("unknown")).thenReturn(Optional.empty());

        assertThrows(UrlNotFoundException.class, () -> urlService.getOriginalUrl("unknown", null));
    }

    @Test
    @DisplayName("Should throw UrlExpiredException when link is past expiration")
    void testGetOriginalUrlExpired() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("url:expired")).thenReturn(null);

        Url url = Url.builder()
                .id(1L)
                .shortCode("expired")
                .originalUrl("https://expired.com")
                .active(true)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        when(urlRepository.findByShortCodeAndActiveTrue("expired")).thenReturn(Optional.of(url));

        assertThrows(UrlExpiredException.class, () -> urlService.getOriginalUrl("expired", null));
    }
}
