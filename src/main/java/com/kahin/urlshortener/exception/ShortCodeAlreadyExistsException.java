package com.kahin.urlshortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a custom short code is already in use.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ShortCodeAlreadyExistsException extends RuntimeException {

    public ShortCodeAlreadyExistsException(String shortCode) {
        super("Short code already exists: " + shortCode);
    }
}
