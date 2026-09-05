package com.urlshortener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * URL Shortener — Production-grade URL shortening service.
 *
 * Features:
 * - Base62 short code generation
 * - Redis caching for fast redirects
 * - IP-based rate limiting
 * - Click analytics (IP, User-Agent, Referer, timestamp)
 * - TTL support for expiring links
 * - Docker + Nginx load balancing deployment
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class UrlShortenerApplication {

    public static void main(String[] args) {
        SpringApplication.run(UrlShortenerApplication.class, args);
    }
}
