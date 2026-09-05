package com.urlshortener.util;

import lombok.Builder;
import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * Lightweight parser for client User-Agent strings.
 * Extracts browser name, operating system, and device type without heavy external dependencies.
 */
@Component
public class UserAgentParser {

    @Getter
    @Builder
    public static class ClientInfo {
        private final String browser;
        private final String os;
        private final String deviceType;
    }

    public ClientInfo parse(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return ClientInfo.builder()
                    .browser("Unknown")
                    .os("Unknown")
                    .deviceType("Unknown")
                    .build();
        }

        String ua = userAgent.toLowerCase();

        // 1. Device Type
        String deviceType = "Desktop";
        if (ua.contains("mobile") || ua.contains("iphone") || ua.contains("ipod") || (ua.contains("android") && !ua.contains("tablet"))) {
            deviceType = "Mobile";
        } else if (ua.contains("tablet") || ua.contains("ipad")) {
            deviceType = "Tablet";
        } else if (ua.contains("bot") || ua.contains("crawler") || ua.contains("spider") || ua.contains("curl") || ua.contains("postman")) {
            deviceType = "Bot";
        }

        // 2. Operating System
        String os = "Unknown";
        if (ua.contains("windows nt 10.0")) {
            os = "Windows 10/11";
        } else if (ua.contains("windows")) {
            os = "Windows";
        } else if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ipod")) {
            os = "iOS";
        } else if (ua.contains("mac os x") || ua.contains("macintosh")) {
            os = "macOS";
        } else if (ua.contains("android")) {
            os = "Android";
        } else if (ua.contains("linux")) {
            os = "Linux";
        }

        // 3. Browser
        String browser = "Unknown";
        if (ua.contains("edg/") || ua.contains("edge/")) {
            browser = "Edge";
        } else if (ua.contains("opr/") || ua.contains("opera")) {
            browser = "Opera";
        } else if (ua.contains("chrome/") && !ua.contains("chromium")) {
            browser = "Chrome";
        } else if (ua.contains("firefox/")) {
            browser = "Firefox";
        } else if (ua.contains("safari/") && !ua.contains("chrome")) {
            browser = "Safari";
        } else if (ua.contains("curl")) {
            browser = "cURL";
        } else if (ua.contains("postman")) {
            browser = "Postman";
        }

        return ClientInfo.builder()
                .browser(browser)
                .os(os)
                .deviceType(deviceType)
                .build();
    }
}
