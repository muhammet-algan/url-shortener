package com.kahin.urlshortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a URL has passed its TTL expiration date.
 * Returns HTTP 410 Gone to indicate the resource existed but is no longer available.
 */
@ResponseStatus(HttpStatus.GONE)
public class UrlExpiredException extends RuntimeException {

    public UrlExpiredException(String shortCode) {
        super("URL has expired for short code: " + shortCode);
    }
}
