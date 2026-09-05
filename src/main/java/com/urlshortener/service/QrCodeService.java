package com.urlshortener.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for generating QR codes for shortened URLs.
 * Uses the ZXing library with high error correction and configurable dimensions.
 */
@Service
@Slf4j
public class QrCodeService {

    private static final int DEFAULT_WIDTH = 300;
    private static final int DEFAULT_HEIGHT = 300;

    /**
     * Generate a QR Code as PNG byte array.
     *
     * @param text content to encode into QR Code
     * @param width image width in pixels
     * @param height image height in pixels
     * @return PNG image bytes
     */
    public byte[] generateQrCode(String text, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();

            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 2);

            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate QR Code for text: {}", text, e);
            throw new RuntimeException("QR Code generation failed", e);
        }
    }

    /**
     * Generate a standard 300x300 PNG QR Code.
     *
     * @param text content to encode
     * @return PNG bytes
     */
    public byte[] generateQrCode(String text) {
        return generateQrCode(text, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }
}
