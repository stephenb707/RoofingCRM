package com.roofingcrm.service.accounting;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Extracts embedded text from PDF receipts (when a text layer exists).
 */
@Component
public class PdfReceiptTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(PdfReceiptTextExtractor.class);

    public String extractEmbeddedText(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            return "";
        }
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            if (document.getNumberOfPages() == 0) {
                return "";
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setStartPage(1);
            stripper.setEndPage(document.getNumberOfPages());
            String text = stripper.getText(document);
            return text == null ? "" : text.trim();
        } catch (IOException ex) {
            log.debug("PDF embedded text extraction failed: {}", ex.getMessage());
            return "";
        }
    }
}
