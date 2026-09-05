package com.urlshortener.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when the provided URL fails validation (malformed, invalid scheme, etc.).
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidUrlException extends RuntimeException {

    public InvalidUrlException(String url) {
        super("Invalid URL provided: " + url);
    }
}
