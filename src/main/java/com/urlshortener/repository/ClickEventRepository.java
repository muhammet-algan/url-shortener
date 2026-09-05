package com.urlshortener.repository;

import com.urlshortener.entity.ClickEvent;
import com.urlshortener.entity.Url;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for click event analytics data.
 */
@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    /**
     * Count total clicks for a specific URL.
     */
    long countByUrl(Url url);

    /**
     * Get recent click events for a URL, ordered by most recent first.
     * Uses Pageable for limiting results.
     */
    List<ClickEvent> findByUrlOrderByClickedAtDesc(Url url, Pageable pageable);

    /**
     * Get click count grouped by hour for the last 24 hours (for analytics charts).
     */
    @Query("SELECT FUNCTION('date_trunc', 'hour', c.clickedAt) as hour, COUNT(c) as clicks " +
           "FROM ClickEvent c WHERE c.url = :url AND c.clickedAt > :since " +
           "GROUP BY FUNCTION('date_trunc', 'hour', c.clickedAt) ORDER BY hour")
    List<Object[]> getHourlyClickStats(@Param("url") Url url, @Param("since") LocalDateTime since);

    /**
     * Get unique visitor count (by IP) for a URL.
     */
    @Query("SELECT COUNT(DISTINCT c.ipAddress) FROM ClickEvent c WHERE c.url = :url")
    long countUniqueVisitors(@Param("url") Url url);

    /**
     * Get top referers for a URL.
     */
    @Query("SELECT c.referer, COUNT(c) FROM ClickEvent c WHERE c.url = :url AND c.referer IS NOT NULL " +
           "GROUP BY c.referer ORDER BY COUNT(c) DESC")
    List<Object[]> getTopReferers(@Param("url") Url url, Pageable pageable);
}
