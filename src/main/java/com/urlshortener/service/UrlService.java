package com.urlshortener.service;

import com.urlshortener.dto.AnalyticsResponse;
import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.dto.UrlResponse;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Core service interface for URL shortening operations.
 * Defines the contract for creating, resolving, analyzing, and managing short URLs.
 */
public interface UrlService {

    /**
     * Create a new shortened URL.
     *
     * @param request the creation request containing the original URL and options
     * @return response containing the generated short URL details
     */
    UrlResponse createShortUrl(CreateUrlRequest request);

    /**
     * Resolve a short code to the original URL and record the click event.
     * This method is called during redirect operations.
     *
     * @param shortCode the short code to resolve
     * @param httpRequest the HTTP request (for analytics data extraction)
     * @return the original URL to redirect to
     */
    String resolveAndTrack(String shortCode, HttpServletRequest httpRequest);

    /**
     * Get comprehensive analytics for a shortened URL.
     *
     * @param shortCode the short code to get analytics for
     * @return analytics data including clicks, visitors, referers
     */
    AnalyticsResponse getAnalytics(String shortCode);

    /**
     * Soft-delete a shortened URL by deactivating it.
     *
     * @param shortCode the short code to deactivate
     */
    void deleteUrl(String shortCode);
}
