package com.urlshortener.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Base62Encoder Unit Tests")
class Base62EncoderTest {

    private Base62Encoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new Base62Encoder();
    }

    @Test
    @DisplayName("Should generate code of requested length")
    void testGenerateCodeLength() {
        assertEquals(6, encoder.generateCode(6).length());
        assertEquals(8, encoder.generateCode(8).length());
        assertEquals(12, encoder.generateCode(12).length());
    }

    @Test
    @DisplayName("Generated code should only contain valid Base62 characters")
    void testGenerateCodeValidCharacters() {
        for (int i = 0; i < 100; i++) {
            String code = encoder.generateCode(6);
            assertTrue(encoder.isValidBase62(code), "Code should be valid Base62: " + code);
        }
    }

    @Test
    @DisplayName("Generated codes should have high randomness and low collision probability")
    void testRandomnessNoCollisions() {
        Set<String> generatedCodes = new HashSet<>();
        int iterations = 1000;
        for (int i = 0; i < iterations; i++) {
            generatedCodes.add(encoder.generateCode(8));
        }
        assertEquals(iterations, generatedCodes.size(), "1000 generated codes should all be unique");
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc123", "ABC123", "a1B2c3", "000000", "ZZZZZZ"})
    @DisplayName("Valid Base62 strings should return true")
    void testValidBase62(String code) {
        assertTrue(encoder.isValidBase62(code));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc-123", "abc_123", "abc@123", "abc 123", "abc.123", "üöç123", "abc/123"})
    @DisplayName("Invalid Base62 strings with special characters should return false")
    void testInvalidBase62(String code) {
        assertFalse(encoder.isValidBase62(code));
    }

    @Test
    @DisplayName("Null and empty strings should return false")
    void testNullAndEmpty() {
        assertFalse(encoder.isValidBase62(null));
        assertFalse(encoder.isValidBase62(""));
    }
}
