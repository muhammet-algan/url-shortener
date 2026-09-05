package com.kahin.urlshortener.repository;

import com.kahin.urlshortener.entity.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for URL entity operations.
 * Provides optimized queries for high-traffic redirect scenarios.
 */
@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {

    /**
     * Find an active URL by its short code.
     * This is the primary query used during redirects.
     */
    Optional<Url> findByShortCodeAndActiveTrue(String shortCode);

    /**
     * Find a URL by short code regardless of active status (for admin/analytics).
     */
    Optional<Url> findByShortCode(String shortCode);

    /**
     * Check if a short code already exists.
     */
    boolean existsByShortCode(String shortCode);

    /**
     * Atomically increment click count and update last accessed time.
     * Uses JPQL to avoid read-modify-write race conditions under concurrent load.
     */
    @Modifying
    @Query("UPDATE Url u SET u.clickCount = u.clickCount + 1, u.lastAccessedAt = :now WHERE u.shortCode = :shortCode")
    void incrementClickCount(@Param("shortCode") String shortCode, @Param("now") LocalDateTime now);

    /**
     * Find all expired but still active URLs — used by the cleanup scheduler.
     */
    List<Url> findByExpiresAtBeforeAndActiveTrue(LocalDateTime now);

    /**
     * Batch deactivate expired URLs.
     */
    @Modifying
    @Query("UPDATE Url u SET u.active = false WHERE u.expiresAt < :now AND u.active = true")
    int deactivateExpiredUrls(@Param("now") LocalDateTime now);
}
