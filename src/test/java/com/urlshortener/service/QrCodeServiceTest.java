package com.urlshortener.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("QrCodeService Unit Tests")
class QrCodeServiceTest {

    private QrCodeService qrCodeService;

    @BeforeEach
    void setUp() {
        qrCodeService = new QrCodeService();
    }

    @Test
    @DisplayName("Should generate valid PNG image bytes for given URL")
    void testGenerateQrCodeSuccess() {
        byte[] qrBytes = qrCodeService.generateQrCode("https://example.com/short123");

        assertNotNull(qrBytes);
        assertTrue(qrBytes.length > 0);

        // PNG Header magic bytes: 0x89, 0x50 ('P'), 0x4E ('N'), 0x47 ('G')
        assertEquals((byte) 0x89, qrBytes[0]);
        assertEquals((byte) 0x50, qrBytes[1]);
        assertEquals((byte) 0x4E, qrBytes[2]);
        assertEquals((byte) 0x47, qrBytes[3]);
    }

    @Test
    @DisplayName("Should generate QR code with custom dimensions")
    void testGenerateQrCodeCustomSize() {
        byte[] smallQr = qrCodeService.generateQrCode("https://example.com", 150, 150);
        byte[] largeQr = qrCodeService.generateQrCode("https://example.com", 500, 500);

        assertNotNull(smallQr);
        assertNotNull(largeQr);
        assertTrue(smallQr.length > 0);
        assertTrue(largeQr.length > 0);
    }
}
