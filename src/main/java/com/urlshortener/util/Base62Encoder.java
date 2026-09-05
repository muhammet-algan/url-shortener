package com.urlshortener.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates short codes using Base62 encoding (0-9, a-z, A-Z).
 *
 * With 6 characters: 62^6 = 56,800,235,584 possible combinations.
 * This is more than enough for most URL shortener use cases.
 *
 * Uses {@link SecureRandom} for cryptographically strong randomness,
 * preventing short code prediction attacks.
 */
@Component
public class Base62Encoder {

    private static final String BASE62_CHARS =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generate a random Base62 short code of the specified length.
     *
     * @param length desired code length (typically 6)
     * @return random Base62 string
     */
    public String generateCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(BASE62_CHARS.charAt(RANDOM.nextInt(BASE62_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * Validate that a string contains only Base62 characters.
     *
     * @param code the code to validate
     * @return true if the code is valid Base62
     */
    public boolean isValidBase62(String code) {
        if (code == null || code.isEmpty()) {
            return false;
        }
        for (char c : code.toCharArray()) {
            if (BASE62_CHARS.indexOf(c) == -1) {
                return false;
            }
        }
        return true;
    }
}
