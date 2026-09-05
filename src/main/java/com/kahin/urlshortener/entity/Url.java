package com.kahin.urlshortener.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Core entity representing a shortened URL.
 *
 * Each record maps a unique short code to its original URL,
 * tracks click statistics, and optionally expires after a TTL.
 */
@Entity
@Table(name = "urls", indexes = {
        @Index(name = "idx_short_code", columnList = "shortCode", unique = true),
        @Index(name = "idx_expires_at", columnList = "expiresAt"),
        @Index(name = "idx_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Url {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The unique short code (e.g., "ab12Cd").
     * Indexed for fast lookup during redirects.
     */
    @Column(nullable = false, unique = true, length = 12)
    private String shortCode;

    /**
     * The original (long) URL to redirect to.
     */
    @Column(nullable = false, length = 2048)
    private String originalUrl;

    /**
     * Timestamp when this URL was created.
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Optional expiration timestamp. Null means the URL never expires.
     */
    @Column
    private LocalDateTime expiresAt;

    /**
     * Total number of times this short URL has been accessed.
     * Updated atomically via JPQL to avoid race conditions.
     */
    @Column(nullable = false)
    @Builder.Default
    private Long clickCount = 0L;

    /**
     * Timestamp of the most recent access/redirect.
     */
    @Column
    private LocalDateTime lastAccessedAt;

    /**
     * Soft-delete flag. Inactive URLs return 410 Gone.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Check if this URL has expired based on current time.
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}
