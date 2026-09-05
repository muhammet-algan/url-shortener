package com.kahin.urlshortener.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Records each individual click/visit on a shortened URL.
 * Used for detailed analytics: IP tracking, user-agent analysis,
 * referer tracking, and time-series click data.
 */
@Entity
@Table(name = "click_events", indexes = {
        @Index(name = "idx_click_url_id", columnList = "url_id"),
        @Index(name = "idx_click_timestamp", columnList = "clickedAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The URL that was clicked. Many clicks belong to one URL.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "url_id", nullable = false)
    private Url url;

    /**
     * Exact timestamp of the click event.
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime clickedAt;

    /**
     * IP address of the visitor.
     * Supports both IPv4 and IPv6 formats.
     */
    @Column(length = 45)
    private String ipAddress;

    /**
     * Browser/client User-Agent string.
     */
    @Column(length = 512)
    private String userAgent;

    /**
     * HTTP Referer header — where the user came from.
     */
    @Column(length = 2048)
    private String referer;
}
