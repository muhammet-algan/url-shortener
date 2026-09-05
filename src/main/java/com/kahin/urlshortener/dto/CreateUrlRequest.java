package com.kahin.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Request DTO for creating a new shortened URL.
 *
 * Example JSON:
 * {
 *   "originalUrl": "https://example.com/very-long-path",
 *   "ttlHours": 168,
 *   "customCode": "mylink"
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUrlRequest {

    /**
     * The original URL to shorten. Must be a valid URL.
     */
    @NotBlank(message = "Original URL is required")
    @Size(max = 2048, message = "URL must not exceed 2048 characters")
    private String originalUrl;

    /**
     * Optional: Time-to-live in hours. The link will expire after this many hours.
     * If null, the system default TTL is applied.
     */
    private Long ttlHours;

    /**
     * Optional: Specific expiration date/time. Takes precedence over ttlHours.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;

    /**
     * Optional: Custom short code. Must be 3-12 alphanumeric characters.
     * If not provided, a random Base62 code is generated.
     */
    @Size(min = 3, max = 12, message = "Custom code must be between 3 and 12 characters")
    private String customCode;
}
