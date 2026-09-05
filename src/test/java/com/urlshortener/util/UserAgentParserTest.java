package com.urlshortener.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserAgentParser Unit Tests")
class UserAgentParserTest {

    private UserAgentParser parser;

    @BeforeEach
    void setUp() {
        parser = new UserAgentParser();
    }

    @Test
    @DisplayName("Should detect Windows and Chrome")
    void testWindowsChrome() {
        String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";
        UserAgentParser.ClientInfo info = parser.parse(ua);

        assertEquals("Chrome", info.getBrowser());
        assertEquals("Windows 10/11", info.getOs());
        assertEquals("Desktop", info.getDeviceType());
    }

    @Test
    @DisplayName("Should detect macOS and Safari")
    void testMacOSSafari() {
        String ua = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2.1 Safari/605.1.15";
        UserAgentParser.ClientInfo info = parser.parse(ua);

        assertEquals("Safari", info.getBrowser());
        assertEquals("macOS", info.getOs());
        assertEquals("Desktop", info.getDeviceType());
    }

    @Test
    @DisplayName("Should detect iPhone and iOS")
    void testIPhoneMobile() {
        String ua = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1";
        UserAgentParser.ClientInfo info = parser.parse(ua);

        assertEquals("Safari", info.getBrowser());
        assertEquals("iOS", info.getOs());
        assertEquals("Mobile", info.getDeviceType());
    }

    @Test
    @DisplayName("Should detect Android mobile device")
    void testAndroidMobile() {
        String ua = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36";
        UserAgentParser.ClientInfo info = parser.parse(ua);

        assertEquals("Chrome", info.getBrowser());
        assertEquals("Android", info.getOs());
        assertEquals("Mobile", info.getDeviceType());
    }

    @Test
    @DisplayName("Should detect iPad tablet")
    void testIPadTablet() {
        String ua = "Mozilla/5.0 (iPad; CPU OS 16_5 like Mac OS X) AppleWebKit/605.1.15";
        UserAgentParser.ClientInfo info = parser.parse(ua);

        assertEquals("iOS", info.getOs());
        assertEquals("Tablet", info.getDeviceType());
    }

    @Test
    @DisplayName("Should handle null or empty User-Agent gracefully")
    void testNullOrEmptyUserAgent() {
        UserAgentParser.ClientInfo nullInfo = parser.parse(null);
        assertEquals("Unknown", nullInfo.getBrowser());
        assertEquals("Unknown", nullInfo.getOs());
        assertEquals("Unknown", nullInfo.getDeviceType());

        UserAgentParser.ClientInfo emptyInfo = parser.parse("   ");
        assertEquals("Unknown", emptyInfo.getBrowser());
    }
}
