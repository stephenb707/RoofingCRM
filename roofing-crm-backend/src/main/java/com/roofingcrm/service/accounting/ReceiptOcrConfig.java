package com.roofingcrm.service.accounting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Optional Tesseract OCR bean for future use. {@link ReceiptExtractionServiceImpl} does not use
 * {@link ReceiptOcrClient}; extraction is vision-only.
 */
@Configuration
public class ReceiptOcrConfig {

    private static final Logger log = LoggerFactory.getLogger(ReceiptOcrConfig.class);

    @Bean
    ReceiptOcrClient receiptOcrClient(ReceiptOcrProperties properties) {
        boolean enabled = properties.isEnabled();
        String provider = normalize(properties.getProvider());

        if (!enabled) {
            return noop("OCR disabled (app.ai.receipt-ocr.enabled=false).");
        }
        if (!"tesseract".equals(provider)) {
            return noop("OCR provider not supported: " + properties.getProvider() + ". Use tesseract.");
        }
        try {
            ReceiptOcrClient client = new TesseractReceiptOcrClient(properties);
            log.info(
                    "Receipt OCR config: enabled=true, provider=tesseract, tessdataPathConfigured={}, language={}, selectedClient=TESSERACT",
                    properties.getTessdataPath() != null && !properties.getTessdataPath().isBlank(),
                    properties.getLanguage());
            return client;
        } catch (RuntimeException ex) {
            log.warn("Receipt OCR Tesseract client failed to initialize: {}. Falling back to NOOP.", ex.getMessage());
            return noop("Tesseract OCR initialization failed: " + ex.getMessage());
        }
    }

    private static ReceiptOcrClient noop(String reason) {
        log.warn("Receipt OCR config: selectedClient=NOOP, reason={}", reason);
        return new NoopReceiptOcrClient(reason);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase();
    }
}
