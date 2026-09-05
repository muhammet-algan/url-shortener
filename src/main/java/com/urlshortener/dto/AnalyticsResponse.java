package com.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive analytics response for a shortened URL.
 * Includes total clicks, unique visitors, recent click details,
 * top referers, and hourly click distribution.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsResponse {

    private String shortCode;
    private String shortUrl;
    private String originalUrl;

    /** Total number of clicks/redirects. */
    private long totalClicks;

    /** Number of unique visitors (by IP). */
    private long uniqueVisitors;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastAccessedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;

    private boolean active;

    /** Most recent click events (limited to 50). */
    private List<ClickDetail> recentClicks;

    /** Top referer sources with click counts. */
    private Map<String, Long> topReferers;

    /** Browser distribution breakdown. */
    private Map<String, Long> topBrowsers;

    /** Operating system distribution breakdown. */
    private Map<String, Long> topOperatingSystems;

    /** Device type breakdown (Desktop, Mobile, Tablet). */
    private Map<String, Long> topDevices;
}
