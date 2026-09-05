package com.urlshortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a short code does not map to any existing URL.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class UrlNotFoundException extends RuntimeException {

    public UrlNotFoundException(String shortCode) {
        super("URL not found for short code: " + shortCode);
    }
}
