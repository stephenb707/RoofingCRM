package com.roofingcrm.service.accounting;

import com.roofingcrm.domain.entity.Attachment;
import com.roofingcrm.domain.enums.ReceiptAmountConfidence;
import com.roofingcrm.domain.enums.ReceiptFieldConfidence;
import com.roofingcrm.domain.enums.ReceiptTotalSource;
import com.roofingcrm.domain.enums.ReceiptExtractionStatus;
import com.roofingcrm.storage.AttachmentStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReceiptExtractionServiceImplTest {

    @Mock
    private AttachmentStorageService attachmentStorageService;
    @Mock
    private ReceiptExtractionClient receiptExtractionClient;
    @Mock
    private ReceiptAmountCandidateExtractor amountCandidateExtractor;
    @Mock
    private ReceiptExtractionDecisionService extractionDecisionService;
    @Mock
    private PdfReceiptTextExtractor pdfReceiptTextExtractor;

    private ReceiptExtractionServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(pdfReceiptTextExtractor.extractEmbeddedText(any())).thenReturn("");

        service = new ReceiptExtractionServiceImpl(
                attachmentStorageService,
                receiptExtractionClient,
                amountCandidateExtractor,
                extractionDecisionService,
                new ReceiptImagePreprocessor(),
                new ReceiptSummaryRegionExtractor(),
                new SummaryFieldConsensusService(),
                pdfReceiptTextExtractor,
                new ReceiptDateCandidateRanker(),
                new ReceiptExtractionProperties()
        );
    }

    @Test
    void extractReceipt_fullImageSucceedsWhenSummaryVariantFails() throws Exception {
        Attachment receipt = receipt();
        when(attachmentStorageService.loadAsStream(anyString())).thenReturn(stream(testImageBytes()));
        when(receiptExtractionClient.extract(any())).thenReturn(fullResult());
        when(receiptExtractionClient.extractSummary(any()))
                .thenThrow(new ReceiptExtractionProviderException("summary failed"));
        when(amountCandidateExtractor.extractCandidates(any())).thenReturn(emptyCandidates());
        when(extractionDecisionService.decideAmount(any(), any(), any(), anyBoolean(), any())).thenReturn(highDecision());

        ReceiptExtractionService.ExtractionDraft result = service.extractReceipt(receipt);

        assertEquals(ReceiptExtractionStatus.COMPLETED, result.status());
        assertEquals(new BigDecimal("1564.38"), result.amount());
        assertNull(result.error());
        verify(receiptExtractionClient, times(1)).extract(any());
        verify(receiptExtractionClient, times(3)).extractSummary(any());
    }

    @Test
    void extractReceipt_highConfidenceAfterStageOneSkipsFallbackVariants() throws Exception {
        Attachment receipt = receipt();
        when(attachmentStorageService.loadAsStream(anyString())).thenReturn(stream(testImageBytes()));
        when(receiptExtractionClient.extract(any())).thenReturn(fullResult());
        when(receiptExtractionClient.extractSummary(any())).thenReturn(summaryResult());
        when(amountCandidateExtractor.extractCandidates(any())).thenReturn(emptyCandidates());
        when(extractionDecisionService.decideAmount(any(), any(), any(), anyBoolean(), any())).thenReturn(highDecision());

        ReceiptExtractionService.ExtractionDraft result = service.extractReceipt(receipt);

        assertEquals(ReceiptExtractionStatus.COMPLETED, result.status());
        verify(receiptExtractionClient, times(3)).extractSummary(any());
        verify(extractionDecisionService, times(1)).decideAmount(any(), any(), any(), anyBoolean(), any());
    }

    @Test
    void extractReceipt_lowConfidenceAfterStageOneInvokesFallbackVariants() throws Exception {
        Attachment receipt = receipt();
        when(attachmentStorageService.loadAsStream(anyString())).thenReturn(stream(testImageBytes()));
        when(receiptExtractionClient.extract(any())).thenReturn(fullResult());
        when(receiptExtractionClient.extractSummary(any()))
                .thenReturn(summaryResult())
                .thenReturn(summaryResult())
                .thenReturn(summaryResult())
                .thenReturn(summaryResult())
                .thenReturn(summaryResult());
        when(amountCandidateExtractor.extractCandidates(any())).thenReturn(emptyCandidates());
        when(extractionDecisionService.decideAmount(any(), any(), any(), anyBoolean(), any()))
                .thenReturn(lowDecision())
                .thenReturn(highDecision());

        ReceiptExtractionService.ExtractionDraft result = service.extractReceipt(receipt);

        assertEquals(ReceiptExtractionStatus.COMPLETED, result.status());
        verify(receiptExtractionClient, times(5)).extractSummary(any());
        verify(extractionDecisionService, times(2)).decideAmount(any(), any(), any(), anyBoolean(), any());
    }

    @Test
    void extractReceipt_allProviderAttemptsFailReturnsProviderFailure() throws Exception {
        Attachment receipt = receipt();
        when(attachmentStorageService.loadAsStream(anyString())).thenReturn(stream(testImageBytes()));
        when(receiptExtractionClient.extract(any()))
                .thenThrow(new ReceiptExtractionProviderException("full failed"));
        when(receiptExtractionClient.extractSummary(any()))
                .thenThrow(new ReceiptExtractionProviderException("summary failed"));

        ReceiptExtractionService.ExtractionDraft result = service.extractReceipt(receipt);

        assertEquals(ReceiptExtractionStatus.FAILED, result.status());
        assertEquals("We couldn't reach the receipt extraction provider. You can retry or enter it manually.", result.error());
        verify(receiptExtractionClient, times(1)).extract(any());
        verify(receiptExtractionClient, times(3)).extractSummary(any());
    }

    private Attachment receipt() {
        Attachment receipt = new Attachment();
        receipt.setId(UUID.randomUUID());
        receipt.setFileName("receipt.png");
        receipt.setContentType("image/png");
        receipt.setStorageKey("receipts/test.png");
        return receipt;
    }

    private InputStream stream(byte[] bytes) {
        return new ByteArrayInputStream(bytes);
    }

    private byte[] testImageBytes() throws IOException {
        BufferedImage source = new BufferedImage(800, 1200, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = source.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
            graphics.setColor(Color.BLACK);
            graphics.drawString("SUBTOTAL 1455.24", 120, 1000);
            graphics.drawString("TAX 109.14", 120, 1040);
            graphics.drawString("TOTAL 1564.38", 120, 1080);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(source, "png", outputStream);
        return outputStream.toByteArray();
    }

    private ReceiptExtractionClient.ExtractedReceiptData fullResult() {
        return new ReceiptExtractionClient.ExtractedReceiptData(
                "ABC Supply",
                "2026-03-31",
                new BigDecimal("1455.24"),
                new BigDecimal("109.14"),
                new BigDecimal("1564.38"),
                new BigDecimal("1564.38"),
                new BigDecimal("1564.38"),
                null,
                "notes",
                85,
                "TOTAL 1564.38"
        );
    }

    private ReceiptExtractionClient.ExtractedReceiptData summaryResult() {
        return new ReceiptExtractionClient.ExtractedReceiptData(
                null,
                null,
                new BigDecimal("1455.24"),
                new BigDecimal("109.14"),
                new BigDecimal("1564.38"),
                new BigDecimal("1564.38"),
                null,
                null,
                null,
                88,
                "SUBTOTAL 1455.24 TAX 109.14 TOTAL 1564.38"
        );
    }

    private ReceiptAmountCandidateExtractor.CandidateExtractionResult emptyCandidates() {
        return new ReceiptAmountCandidateExtractor.CandidateExtractionResult(
                List.of(),
                List.of(),
                new ReceiptAmountCandidateExtractor.DetectedSummaryAmounts(null, null, null, null)
        );
    }

    private ReceiptExtractionDecisionService.ReceiptAmountDecision highDecision() {
        return new ReceiptExtractionDecisionService.ReceiptAmountDecision(
                new BigDecimal("1564.38"),
                new BigDecimal("1455.24"),
                ReceiptFieldConfidence.HIGH,
                new BigDecimal("109.14"),
                ReceiptFieldConfidence.HIGH,
                new BigDecimal("1564.38"),
                ReceiptFieldConfidence.HIGH,
                new BigDecimal("1564.38"),
                ReceiptFieldConfidence.MEDIUM,
                new BigDecimal("1564.38"),
                List.of(new BigDecimal("1564.38")),
                ReceiptAmountConfidence.HIGH,
                false,
                new BigDecimal("7.5"),
                ReceiptFieldConfidence.HIGH,
                true,
                List.of(),
                ReceiptTotalSource.SUMMARY_MATH_VALIDATED
        );
    }

    private ReceiptExtractionDecisionService.ReceiptAmountDecision lowDecision() {
        return new ReceiptExtractionDecisionService.ReceiptAmountDecision(
                new BigDecimal("1563.65"),
                new BigDecimal("1455.24"),
                ReceiptFieldConfidence.MEDIUM,
                new BigDecimal("108.41"),
                ReceiptFieldConfidence.LOW,
                new BigDecimal("1563.65"),
                ReceiptFieldConfidence.LOW,
                new BigDecimal("1563.65"),
                ReceiptFieldConfidence.LOW,
                new BigDecimal("1563.65"),
                List.of(new BigDecimal("1563.65"), new BigDecimal("1564.38")),
                ReceiptAmountConfidence.LOW,
                false,
                null,
                ReceiptFieldConfidence.UNKNOWN,
                false,
                List.of("Please review the total before saving."),
                ReceiptTotalSource.NONE
        );
    }
}
