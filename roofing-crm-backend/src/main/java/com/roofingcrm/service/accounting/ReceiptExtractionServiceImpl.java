package com.roofingcrm.service.accounting;

import com.roofingcrm.domain.entity.Attachment;
import com.roofingcrm.domain.enums.JobCostCategory;
import com.roofingcrm.domain.enums.ReceiptAmountConfidence;
import com.roofingcrm.domain.enums.ReceiptFieldConfidence;
import com.roofingcrm.domain.enums.ReceiptExtractionStatus;
import com.roofingcrm.domain.enums.ReceiptTotalSource;
import com.roofingcrm.storage.AttachmentStorageService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;

/**
 * Vision-first receipt extraction: OpenAI full-image and summary-crop calls drive amounts and fields.
 */
@Service
public class ReceiptExtractionServiceImpl implements ReceiptExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ReceiptExtractionServiceImpl.class);
    private static final int MAX_RAW_TEXT_LENGTH = 4000;
    /**
     * Multiple crop/variant pairs so {@link SummaryFieldConsensusService} can vote (see
     * {@code SummaryFieldConsensusServiceTest}). A single attempt regresses to one noisy vision read
     * and wrong subtotal/tax/total/amountPaid for typical receipts.
     */
    private static final List<SummaryAttemptPlan> PRIMARY_SUMMARY_ATTEMPTS = List.of(
            new SummaryAttemptPlan("tight", "baseline"),
            new SummaryAttemptPlan("large", "threshold"),
            new SummaryAttemptPlan("expanded", "sharpened")
    );
    /** Extra attempts when primary consensus/decision is weak or inconsistent. */
    private static final List<SummaryAttemptPlan> FALLBACK_SUMMARY_ATTEMPTS = List.of(
            new SummaryAttemptPlan("large", "baseline"),
            new SummaryAttemptPlan("tight", "threshold")
    );

    private final AttachmentStorageService attachmentStorageService;
    private final ReceiptExtractionClient receiptExtractionClient;
    private final ReceiptAmountCandidateExtractor amountCandidateExtractor;
    private final ReceiptExtractionDecisionService extractionDecisionService;
    private final ReceiptImagePreprocessor receiptImagePreprocessor;
    private final ReceiptSummaryRegionExtractor receiptSummaryRegionExtractor;
    private final SummaryFieldConsensusService summaryFieldConsensusService;
    private final PdfReceiptTextExtractor pdfReceiptTextExtractor;
    private final ReceiptDateCandidateRanker receiptDateCandidateRanker;
    private final ReceiptExtractionProperties receiptExtractionProperties;

    public ReceiptExtractionServiceImpl(AttachmentStorageService attachmentStorageService,
                                        ReceiptExtractionClient receiptExtractionClient,
                                        ReceiptAmountCandidateExtractor amountCandidateExtractor,
                                        ReceiptExtractionDecisionService extractionDecisionService,
                                        ReceiptImagePreprocessor receiptImagePreprocessor,
                                        ReceiptSummaryRegionExtractor receiptSummaryRegionExtractor,
                                        SummaryFieldConsensusService summaryFieldConsensusService,
                                        PdfReceiptTextExtractor pdfReceiptTextExtractor,
                                        ReceiptDateCandidateRanker receiptDateCandidateRanker,
                                        ReceiptExtractionProperties receiptExtractionProperties) {
        this.attachmentStorageService = attachmentStorageService;
        this.receiptExtractionClient = receiptExtractionClient;
        this.amountCandidateExtractor = amountCandidateExtractor;
        this.extractionDecisionService = extractionDecisionService;
        this.receiptImagePreprocessor = receiptImagePreprocessor;
        this.receiptSummaryRegionExtractor = receiptSummaryRegionExtractor;
        this.summaryFieldConsensusService = summaryFieldConsensusService;
        this.pdfReceiptTextExtractor = pdfReceiptTextExtractor;
        this.receiptDateCandidateRanker = receiptDateCandidateRanker;
        this.receiptExtractionProperties = receiptExtractionProperties;
    }

    @Override
    public ExtractionDraft extractReceipt(Attachment receipt) {
        if (receipt.getStorageKey() == null || receipt.getStorageKey().isBlank()) {
            return failed("Receipt file is missing from storage.");
        }

        try (InputStream inputStream = attachmentStorageService.loadAsStream(receipt.getStorageKey())) {
            byte[] bytes = inputStream.readAllBytes();
            ReceiptImagePreprocessor.ProcessedReceiptImage processedImage = toProcessedImage(receipt, bytes);
            List<ReceiptSummaryRegionExtractor.SummaryRegionCrop> summaryCrops =
                    receiptSummaryRegionExtractor.extractVariants(processedImage.image());

            log.info("Receipt extraction (vision-first) preprocessing for {}: original={}x{}, processed={}x{}, summaryCrops={}",
                    receipt.getId(),
                    processedImage.originalWidth(),
                    processedImage.originalHeight(),
                    processedImage.image().getWidth(),
                    processedImage.image().getHeight(),
                    summaryCrops.stream()
                            .map(crop -> "%s[x=%d,y=%d,w=%d,h=%d]".formatted(
                                    crop.id(), crop.x(), crop.y(), crop.width(), crop.height()))
                            .toList());

            String pdfEmbedded = "";
            if ("application/pdf".equalsIgnoreCase(receipt.getContentType() == null ? "" : receipt.getContentType())) {
                pdfEmbedded = pdfReceiptTextExtractor.extractEmbeddedText(bytes);
            }

            ReceiptExtractionClient.ExtractedReceiptData interpret =
                    attemptInterpretation(receipt, normalizeRawText(pdfEmbedded), "");

            ProviderAttemptResult fullAttempt = attemptFullExtraction(receipt, processedImage.image());
            int providerAttemptCount = 1;
            int providerSuccessCount = fullAttempt.result() != null ? 1 : 0;
            List<String> providerFailures = new ArrayList<>();
            if (fullAttempt.failureMessage() != null) {
                providerFailures.add(fullAttempt.failureMessage());
            }

            String visionFullRaw = fullAttempt.result() != null ? fullAttempt.result().rawExtractedText() : null;
            ReceiptExtractionClient.ExtractedReceiptData fullExtracted = mergeFullExtractedVisionFirst(
                    interpret,
                    fullAttempt.result(),
                    normalizeRawText(visionFullRaw));

            List<SummaryFieldConsensusService.SummaryExtractionAttempt> summaryAttempts = new ArrayList<>();
            SummaryAttemptBatch primaryBatch = runSummaryAttempts(receipt, summaryCrops, PRIMARY_SUMMARY_ATTEMPTS);
            providerAttemptCount += primaryBatch.attemptCount();
            providerSuccessCount += primaryBatch.successCount();
            providerFailures.addAll(primaryBatch.failureMessages());
            summaryAttempts.addAll(primaryBatch.attempts());

            if (providerSuccessCount == 0) {
                throw providerFailure(providerFailures, receipt.getId());
            }

            log.info("Receipt extraction (vision-first) for {}: fullVisionOk={}, summaryAttempts={}",
                    receipt.getId(),
                    fullAttempt.result() != null,
                    summaryAttempts.size());

            boolean suppressFullImageNumericEvidence = false;
            ExtractionComputation initialComputation = computeDecision(
                    receipt.getId(),
                    fullExtracted,
                    summaryAttempts,
                    suppressFullImageNumericEvidence);

            SummaryFieldConsensusService.SummaryConsensusResult summaryConsensus = initialComputation.summaryConsensus();
            ReceiptExtractionDecisionService.ReceiptAmountDecision amountDecision = initialComputation.amountDecision();
            String reviewCombinedRawText = initialComputation.reviewCombinedRawText();

            if (shouldRunFallback(summaryConsensus, amountDecision)) {
                log.info("Receipt extraction invoking fallback summary attempts for {}: confidence={}, summaryNotes={}, warnings={}",
                        receipt.getId(),
                        amountDecision.amountConfidence(),
                        summaryConsensus.notes(),
                        amountDecision.warnings());
                SummaryAttemptBatch fallbackBatch = runSummaryAttempts(receipt, summaryCrops, FALLBACK_SUMMARY_ATTEMPTS);
                providerAttemptCount += fallbackBatch.attemptCount();
                providerSuccessCount += fallbackBatch.successCount();
                providerFailures.addAll(fallbackBatch.failureMessages());
                summaryAttempts.addAll(fallbackBatch.attempts());

                if (!fallbackBatch.attempts().isEmpty()) {
                    ExtractionComputation fallbackComputation = computeDecision(
                            receipt.getId(),
                            fullExtracted,
                            summaryAttempts,
                            suppressFullImageNumericEvidence);
                    summaryConsensus = fallbackComputation.summaryConsensus();
                    amountDecision = fallbackComputation.amountDecision();
                    reviewCombinedRawText = fallbackComputation.reviewCombinedRawText();
                }
            }

            log.info("Receipt extraction consensus for {}: summary(subtotal={} {}, tax={} {}, total={} {}, amountPaid={} {}), computedTotal={}, finalAmount={}, finalWarnings={}",
                    receipt.getId(),
                    summaryConsensus.subtotal(),
                    summaryConsensus.subtotalConfidence(),
                    summaryConsensus.tax(),
                    summaryConsensus.taxConfidence(),
                    summaryConsensus.total(),
                    summaryConsensus.totalConfidence(),
                    summaryConsensus.amountPaid(),
                    summaryConsensus.amountPaidConfidence(),
                    amountDecision.computedTotal(),
                    amountDecision.amount(),
                    amountDecision.warnings());
            log.debug("Receipt extraction final money for {}: subtotal={}, tax={}, total={}, amountPaid={}, taxRate={} {}, totalSource={}",
                    receipt.getId(),
                    amountDecision.subtotal(),
                    amountDecision.tax(),
                    amountDecision.total(),
                    amountDecision.amountPaid(),
                    amountDecision.taxRatePercent(),
                    amountDecision.taxRateConfidence(),
                    amountDecision.totalSource());
            log.info("Receipt extraction provider call summary for {}: attemptedCalls={}, successfulCalls={}, failedCalls={}, failureLabels={}",
                    receipt.getId(),
                    providerAttemptCount,
                    providerSuccessCount,
                    providerAttemptCount - providerSuccessCount,
                    providerFailures);

            List<String> mergedWarnings = new ArrayList<>(amountDecision.warnings());

            Instant incurredAt = receiptDateCandidateRanker.resolveIncurredAt(
                    fullExtracted.incurredDate(), reviewCombinedRawText);
            if (incurredAt == null) {
                incurredAt = parseDate(fullExtracted.incurredDate());
            }

            ExtractionDraft draft = new ExtractionDraft(
                    ReceiptExtractionStatus.COMPLETED,
                    Instant.now(),
                    null,
                    normalize(fullExtracted.vendorName()),
                    incurredAt,
                    amountDecision.amount(),
                    amountDecision.subtotal(),
                    amountDecision.tax(),
                    amountDecision.total(),
                    amountDecision.amountPaid(),
                    amountDecision.computedTotal(),
                    amountDecision.subtotalConfidence(),
                    amountDecision.taxConfidence(),
                    amountDecision.totalConfidence(),
                    amountDecision.amountPaidConfidence(),
                    summaryConsensus.subtotal(),
                    summaryConsensus.tax(),
                    summaryConsensus.total(),
                    summaryConsensus.amountPaid(),
                    amountDecision.amountCandidates(),
                    amountDecision.amountConfidence(),
                    fullExtracted.suggestedCategory(),
                    normalize(fullExtracted.notes()),
                    normalizeConfidence(fullExtracted.confidence()),
                    reviewCombinedRawText,
                    normalizeRawText(summaryConsensus.rawText()),
                    mergedWarnings,
                    amountDecision.taxRatePercent()
            );
            log.info(
                    "Receipt extraction final draft for {}: subtotal={}, tax={}, total={}, computedTotal={}, taxRatePercent={}, incurredAt={}",
                    receipt.getId(),
                    draft.extractedSubtotal(),
                    draft.extractedTax(),
                    draft.extractedTotal(),
                    draft.computedTotal(),
                    draft.extractedTaxRatePercent(),
                    draft.incurredAt());
            return draft;
        } catch (ReceiptExtractionUnavailableException ex) {
            log.warn("Receipt extraction unavailable for {}: {}", receipt.getId(), ex.getMessage(), ex);
            return failed("Receipt extraction is disabled or not configured correctly.");
        } catch (ReceiptExtractionProviderException ex) {
            log.warn("Receipt extraction provider request failed for {}: {}", receipt.getId(), ex.getMessage(), ex);
            return failed("We couldn't reach the receipt extraction provider. You can retry or enter it manually.");
        } catch (Exception ex) {
            log.warn("Receipt extraction failed for {}", receipt.getId(), ex);
            return failed("We couldn't reliably extract details from this receipt. You can retry or enter it manually.");
        }
    }

    private ReceiptImagePreprocessor.ProcessedReceiptImage toProcessedImage(Attachment receipt, byte[] bytes) throws IOException {
        String contentType = receipt.getContentType() == null ? "application/octet-stream" : receipt.getContentType();
        BufferedImage image;

        if ("application/pdf".equalsIgnoreCase(contentType)) {
            image = renderPdfPreview(bytes);
        } else if (contentType.startsWith("image/")) {
            image = normalizeImage(bytes);
            if (image == null) {
                throw new IllegalArgumentException("Unsupported receipt image format for extraction");
            }
        } else {
            throw new IllegalArgumentException("Unsupported receipt content type for extraction");
        }

        return receiptImagePreprocessor.preprocess(image);
    }

    private ReceiptExtractionClient.ReceiptVisionDocument toVisionDocument(Attachment receipt,
                                                                           BufferedImage image,
                                                                           String promptContextSuffix,
                                                                           String attemptLabel) throws IOException {
        String context = normalize(receipt.getDescription());
        if (promptContextSuffix != null && !promptContextSuffix.isBlank()) {
            context = context == null ? promptContextSuffix : context + " | " + promptContextSuffix;
        }
        byte[] pngBytes = toPngBytes(image);
        return new ReceiptExtractionClient.ReceiptVisionDocument(
                attemptLabel,
                receipt.getFileName(),
                receipt.getContentType(),
                context,
                "image/png",
                Base64.getEncoder().encodeToString(pngBytes),
                image.getWidth(),
                image.getHeight(),
                pngBytes.length
        );
    }

    private BufferedImage renderPdfPreview(byte[] pdfBytes) throws IOException {
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            if (document.getNumberOfPages() == 0) {
                throw new IllegalArgumentException("Receipt PDF is empty");
            }
            PDFRenderer renderer = new PDFRenderer(document);
            return renderer.renderImageWithDPI(0, 220, ImageType.RGB);
        }
    }

    private BufferedImage normalizeImage(byte[] imageBytes) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes)) {
            return ImageIO.read(inputStream);
        }
    }

    private byte[] toPngBytes(BufferedImage image) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        }
    }

    private Instant parseDate(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        try {
            LocalDate localDate = LocalDate.parse(normalized);
            return localDate.atTime(12, 0).toInstant(ZoneOffset.UTC);
        } catch (Exception ex) {
            return null;
        }
    }

    private Integer normalizeConfidence(Integer confidence) {
        if (confidence == null) {
            return null;
        }
        return Math.max(0, Math.min(100, confidence));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeRawText(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        return normalized.length() <= MAX_RAW_TEXT_LENGTH
                ? normalized
                : normalized.substring(0, MAX_RAW_TEXT_LENGTH) + "...";
    }

    private String combineRawTexts(String fullRawText, String summaryRawText) {
        if (fullRawText == null) {
            return summaryRawText;
        }
        if (summaryRawText == null) {
            return fullRawText;
        }
        return normalizeRawText(fullRawText + "\n\nSummary region (vision consensus):\n" + summaryRawText);
    }

    private ReceiptExtractionClient.ExtractedReceiptData attemptInterpretation(
            Attachment receipt,
            String pdfEmbeddedText,
            String unusedSummary) {
        try {
            return receiptExtractionClient.interpretFromTranscribedText(
                    pdfEmbeddedText == null ? "" : pdfEmbeddedText,
                    unusedSummary,
                    new ReceiptTextInterpretationContext(
                            receipt.getFileName(),
                            receipt.getContentType(),
                            normalize(receipt.getDescription())
                    ));
        } catch (ReceiptExtractionUnavailableException ex) {
            log.debug("Receipt interpretation unavailable: {}", ex.getMessage());
            return emptyExtractedData();
        } catch (ReceiptExtractionProviderException ex) {
            log.warn("Receipt interpretation failed: {}", ex.getMessage());
            return emptyExtractedData();
        }
    }

    /**
     * Vision results take precedence over optional PDF-embedded text interpretation for vendor/date/notes.
     */
    private ReceiptExtractionClient.ExtractedReceiptData mergeFullExtractedVisionFirst(
            ReceiptExtractionClient.ExtractedReceiptData interpret,
            ReceiptExtractionClient.ExtractedReceiptData vision,
            String visionFullRawText) {
        ReceiptExtractionClient.ExtractedReceiptData i = interpret == null ? emptyExtractedData() : interpret;
        ReceiptExtractionClient.ExtractedReceiptData v = vision == null ? emptyExtractedData() : vision;
        String vendor = firstNonNullString(v.vendorName(), i.vendorName());
        String date = firstNonNullString(v.incurredDate(), i.incurredDate());
        JobCostCategory category = v.suggestedCategory() != null ? v.suggestedCategory() : i.suggestedCategory();
        String notes = firstNonNullString(v.notes(), i.notes());
        Integer conf = v.confidence() != null ? v.confidence() : i.confidence();
        return new ReceiptExtractionClient.ExtractedReceiptData(
                vendor,
                date,
                v.subtotal(),
                v.tax(),
                v.total(),
                v.amountPaid(),
                v.suggestedAmount(),
                category,
                notes,
                conf,
                visionFullRawText
        );
    }

    private static String firstNonNullString(String primary, String secondary) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return secondary;
    }

    private ProviderAttemptResult attemptFullExtraction(Attachment receipt, BufferedImage image) throws IOException {
        String attemptLabel = "full-image";
        ReceiptExtractionClient.ReceiptVisionDocument document =
                toVisionDocument(receipt, image, receipt.getContentType(), attemptLabel);
        try {
            log.info("Receipt extraction provider attempt for {} [{}]: dims={}x{}, bytes={}",
                    receipt.getId(),
                    attemptLabel,
                    document.width(),
                    document.height(),
                    document.imageByteSize());
            return new ProviderAttemptResult(
                    receiptExtractionClient.extract(document),
                    null
            );
        } catch (ReceiptExtractionProviderException ex) {
            log.warn("Receipt extraction provider attempt failed for {} [{}]: {}",
                    receipt.getId(),
                    attemptLabel,
                    ex.getMessage());
            return new ProviderAttemptResult(null, attemptLabel + ": " + ex.getMessage());
        }
    }

    private SummaryAttemptBatch runSummaryAttempts(Attachment receipt,
                                                   List<ReceiptSummaryRegionExtractor.SummaryRegionCrop> summaryCrops,
                                                   List<SummaryAttemptPlan> plans) throws IOException {
        List<SummaryFieldConsensusService.SummaryExtractionAttempt> attempts = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        int successCount = 0;
        for (SummaryAttemptPlan plan : plans) {
            ReceiptSummaryRegionExtractor.SummaryRegionCrop crop = findCrop(summaryCrops, plan.cropId());
            if (crop == null) {
                continue;
            }
            ReceiptImagePreprocessor.SummaryImageVariant variant =
                    receiptImagePreprocessor.preprocessSummaryVariant(crop.image(), plan.variantId());
            String attemptLabel = "summary-%s-%s".formatted(crop.id(), variant.id());
            maybeWriteSummaryDebugImage(receipt, attemptLabel, variant.image());
            ReceiptExtractionClient.ReceiptVisionDocument summaryDocument =
                    toVisionDocument(receipt, variant.image(), "summary-region:%s:%s".formatted(crop.id(), variant.id()), attemptLabel);
            log.info(
                    "[summary-vision-debug] receipt={} attempt={} crop={}x{} processed={}x{} pngBytes={}",
                    receipt.getId(),
                    attemptLabel,
                    crop.width(),
                    crop.height(),
                    variant.image().getWidth(),
                    variant.image().getHeight(),
                    summaryDocument.imageByteSize());
            try {
                log.info("Receipt extraction provider attempt for {} [{}]: dims={}x{}, bytes={}",
                        receipt.getId(),
                        attemptLabel,
                        summaryDocument.width(),
                        summaryDocument.height(),
                        summaryDocument.imageByteSize());
                ReceiptExtractionClient.ExtractedReceiptData extracted = receiptExtractionClient.extractSummary(summaryDocument);
                log.info("Receipt extraction summary attempt for {} [{}]: subtotal={}, tax={}, total={}, amountPaid={}",
                        receipt.getId(),
                        attemptLabel,
                        extracted.subtotal(),
                        extracted.tax(),
                        extracted.total(),
                        extracted.amountPaid());
                attempts.add(new SummaryFieldConsensusService.SummaryExtractionAttempt(
                        crop.id(),
                        crop.weight(),
                        variant.id(),
                        variant.weight(),
                        extracted
                ));
                successCount += 1;
            } catch (ReceiptExtractionProviderException ex) {
                log.warn("Receipt extraction summary attempt failed for {} [{}]: {}",
                        receipt.getId(),
                        attemptLabel,
                        ex.getMessage());
                failures.add(attemptLabel + ": " + ex.getMessage());
            }
        }
        return new SummaryAttemptBatch(attempts, failures, plans.size(), successCount);
    }

    private void maybeWriteSummaryDebugImage(Attachment receipt, String attemptLabel, BufferedImage image) {
        if (!receiptExtractionProperties.isDebugWriteSummaryImages()) {
            return;
        }
        try {
            Path dir = Path.of(System.getProperty("java.io.tmpdir"), "roofing-crm-summary-debug");
            Files.createDirectories(dir);
            String safe = attemptLabel.replaceAll("[^a-zA-Z0-9.-]", "_");
            Path file = dir.resolve(receipt.getId() + "-" + safe + ".png");
            ImageIO.write(image, "png", file.toFile());
            log.info("[summary-vision-debug] wrote processed summary image to {}", file.toAbsolutePath());
        } catch (Exception ex) {
            log.warn("Could not write summary debug image: {}", ex.getMessage());
        }
    }

    private ExtractionComputation computeDecision(
            Object receiptId,
            ReceiptExtractionClient.ExtractedReceiptData fullExtracted,
            List<SummaryFieldConsensusService.SummaryExtractionAttempt> summaryAttempts,
            boolean suppressFullImageNumericEvidence) {
        if (log.isDebugEnabled()) {
            for (SummaryFieldConsensusService.SummaryExtractionAttempt a : summaryAttempts) {
                var r = a.result();
                log.debug(
                        "Receipt summary attempt [{}] for {}: weights crop={} variant={} subtotal={} tax={} total={} amountPaid={} modelConfidence={}",
                        "%s/%s".formatted(a.cropId(), a.variantId()),
                        receiptId,
                        a.cropWeight(),
                        a.variantWeight(),
                        r.subtotal(),
                        r.tax(),
                        r.total(),
                        r.amountPaid(),
                        r.confidence());
            }
        }
        SummaryFieldConsensusService.SummaryConsensusResult summaryConsensus =
                summaryFieldConsensusService.buildConsensus(summaryAttempts);
        String candidateSourceText = buildVisionCandidateExtractionText(
                summaryConsensus.rawText(),
                fullExtracted.rawExtractedText());
        ReceiptAmountCandidateExtractor.CandidateExtractionResult candidates =
                amountCandidateExtractor.extractCandidates(normalizeRawText(candidateSourceText));
        log.debug(
                "Receipt amount decision INPUTS for {}: summaryConsensus(subtotal={} {}, tax={} {}, total={} {}, amountPaid={} {}), "
                        + "summaryNotes={}, fullImageVision(subtotal={}, tax={}, total={}, amountPaid={}, suggestedAmount={}, overallConfidence={}), "
                        + "summaryComputedSubtotalPlusTax={}, candidateLineCount={}, detectedSummaryAmounts={}, candidateWarnings={}",
                receiptId,
                summaryConsensus.subtotal(), summaryConsensus.subtotalConfidence(),
                summaryConsensus.tax(), summaryConsensus.taxConfidence(),
                summaryConsensus.total(), summaryConsensus.totalConfidence(),
                summaryConsensus.amountPaid(), summaryConsensus.amountPaidConfidence(),
                summaryConsensus.notes(),
                fullExtracted.subtotal(), fullExtracted.tax(), fullExtracted.total(),
                fullExtracted.amountPaid(), fullExtracted.suggestedAmount(), fullExtracted.confidence(),
                summarySubtotalPlusTaxIfPresent(summaryConsensus),
                candidates.candidates().size(),
                candidates.summaryAmounts(),
                candidates.warnings());
        logReceiptTotalDebugUpstream(receiptId, fullExtracted, summaryAttempts);

        ReceiptExtractionDecisionService.ReceiptAmountDecision amountDecision =
                extractionDecisionService.decideAmount(
                        candidates,
                        fullExtracted,
                        summaryConsensus,
                        suppressFullImageNumericEvidence,
                        summaryAttempts);
        log.info(
                "[receipt-total-debug] winner={} total={} reason={}",
                amountDecision.totalSource(),
                amountDecision.amount(),
                explainTotalSelection(amountDecision.totalSource()));
        log.debug(
                "Receipt amount decision RESULT for {}: totalSource={}, finalAmount={}, subtotal={}, tax={}, total={}, amountPaid={}, "
                        + "taxRatePercent={} {}, amountConfidence={}, computedTotal={}, amountCandidates={}, warnings={}",
                receiptId,
                amountDecision.totalSource(),
                amountDecision.amount(),
                amountDecision.subtotal(), amountDecision.tax(), amountDecision.total(), amountDecision.amountPaid(),
                amountDecision.taxRatePercent(), amountDecision.taxRateConfidence(),
                amountDecision.amountConfidence(), amountDecision.computedTotal(),
                amountDecision.amountCandidates(),
                amountDecision.warnings());
        log.debug("Receipt amount decision WHY for {}: totalSource={} — {}",
                receiptId,
                amountDecision.totalSource(),
                explainTotalSelection(amountDecision.totalSource()));
        String reviewCombined = combineRawTexts(
                normalizeRawText(fullExtracted.rawExtractedText()),
                normalizeRawText(summaryConsensus.rawText()));
        return new ExtractionComputation(summaryConsensus, amountDecision, reviewCombined);
    }

    private static String explainTotalSelection(ReceiptTotalSource totalSource) {
        return switch (totalSource) {
            case SUMMARY_MATH_VALIDATED ->
                    "Summary subtotal+tax equals summary total with sufficient field confidence (highest priority).";
            case COMPUTED_SUBTOTAL_PLUS_TAX ->
                    "Trusted summary subtotal and tax; total = subtotal+tax (summary region).";
            case SUMMARY_TOTAL_AND_PAID_MATCH ->
                    "Summary total matches summary amount paid with sufficient confidence.";
            case BEST_SUMMARY_COHERENT_ATTEMPT ->
                    "Single math-coherent summary crop/variant chosen for total (beats per-field consensus dilution).";
            case VISION_CONSISTENT_WITH_SUMMARY ->
                    "Full-image total matches summary total, computed subtotal+tax, or amount paid.";
            case VISION_TOTAL ->
                    "Full-image vision total used (allowed only when it matches summary subtotal+tax arithmetic if present).";
            case VISION_AMOUNT_PAID ->
                    "Full-image amount paid used as total (no stronger summary total; no line-candidate total).";
            case CANDIDATE_FALLBACK ->
                    "Line-regex candidate (should not occur in current pipeline for final total).";
            case SUGGESTED_AMOUNT_LOW ->
                    "Fallback to low-confidence suggested amount from vision text.";
            case NONE ->
                    "No reliable total; user must review.";
        };
    }

    private void logReceiptTotalDebugUpstream(Object receiptId,
                                              ReceiptExtractionClient.ExtractedReceiptData fullExtracted,
                                              List<SummaryFieldConsensusService.SummaryExtractionAttempt> summaryAttempts) {
        log.info(
                "[receipt-total-debug] attempt=full-image subtotal={} tax={} total={} amountPaid={} confidence={}",
                fullExtracted.subtotal(),
                fullExtracted.tax(),
                fullExtracted.total(),
                fullExtracted.amountPaid(),
                fullExtracted.confidence());
        for (SummaryFieldConsensusService.SummaryExtractionAttempt a : summaryAttempts) {
            var r = a.result();
            log.info(
                    "[receipt-total-debug] attempt=summary-{}-{} subtotal={} tax={} total={} amountPaid={} confidence={}",
                    a.cropId(),
                    a.variantId(),
                    r.subtotal(),
                    r.tax(),
                    r.total(),
                    r.amountPaid(),
                    r.confidence());
        }
    }

    private static BigDecimal summarySubtotalPlusTaxIfPresent(SummaryFieldConsensusService.SummaryConsensusResult s) {
        if (s.subtotal() == null || s.tax() == null) {
            return null;
        }
        return s.subtotal().add(s.tax()).setScale(2, RoundingMode.HALF_UP);
    }

    private String buildVisionCandidateExtractionText(String consensusRaw, String fullVisionRaw) {
        StringBuilder sb = new StringBuilder();
        if (consensusRaw != null && !consensusRaw.isBlank()) {
            sb.append("--- SUMMARY VISION (consensus) ---\n").append(consensusRaw.trim());
        }
        if (fullVisionRaw != null && !fullVisionRaw.isBlank()) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append("--- FULL VISION ---\n").append(fullVisionRaw.trim());
        }
        return sb.length() == 0 ? "" : sb.toString();
    }

    private boolean shouldRunFallback(SummaryFieldConsensusService.SummaryConsensusResult summaryConsensus,
                                      ReceiptExtractionDecisionService.ReceiptAmountDecision amountDecision) {
        if (amountDecision.amountConfidence() != ReceiptAmountConfidence.HIGH) {
            return true;
        }
        if (summaryConsensus.notes().stream()
                .anyMatch(note -> note != null && note.contains("differs from subtotal"))) {
            return true;
        }
        return summaryConsensus.total() != null
                && amountDecision.computedTotal() != null
                && !approximatelyEquals(summaryConsensus.total(), amountDecision.computedTotal());
    }

    private ReceiptSummaryRegionExtractor.SummaryRegionCrop findCrop(List<ReceiptSummaryRegionExtractor.SummaryRegionCrop> summaryCrops,
                                                                     String cropId) {
        for (ReceiptSummaryRegionExtractor.SummaryRegionCrop crop : summaryCrops) {
            if (crop.id().equals(cropId)) {
                return crop;
            }
        }
        return null;
    }

    private ReceiptExtractionClient.ExtractedReceiptData emptyExtractedData() {
        return new ReceiptExtractionClient.ExtractedReceiptData(
                null, null, null, null, null, null, null, null, null, null, null
        );
    }

    private ReceiptExtractionProviderException providerFailure(List<String> providerFailures, Object receiptId) {
        String reason = providerFailures.isEmpty()
                ? "No provider attempts succeeded."
                : String.join(" | ", providerFailures);
        return new ReceiptExtractionProviderException(
                "All receipt extraction provider attempts failed for " + receiptId + ". " + reason
        );
    }

    private boolean approximatelyEquals(BigDecimal left, BigDecimal right) {
        return left != null && right != null && left.subtract(right).abs().compareTo(new BigDecimal("0.02")) <= 0;
    }

    private ExtractionDraft failed(String message) {
        return new ExtractionDraft(ReceiptExtractionStatus.FAILED, Instant.now(), message,
                null, null, null, null, null, null, null,
                null, ReceiptFieldConfidence.UNKNOWN, ReceiptFieldConfidence.UNKNOWN,
                ReceiptFieldConfidence.UNKNOWN, ReceiptFieldConfidence.UNKNOWN,
                null, null, null, null,
                List.of(), ReceiptAmountConfidence.LOW, null, null, null, null, null, List.of(), null);
    }

    private record SummaryAttemptPlan(
            String cropId,
            String variantId
    ) {
    }

    private record ProviderAttemptResult(
            ReceiptExtractionClient.ExtractedReceiptData result,
            String failureMessage
    ) {
    }

    private record SummaryAttemptBatch(
            List<SummaryFieldConsensusService.SummaryExtractionAttempt> attempts,
            List<String> failureMessages,
            int attemptCount,
            int successCount
    ) {
    }

    private record ExtractionComputation(
            SummaryFieldConsensusService.SummaryConsensusResult summaryConsensus,
            ReceiptExtractionDecisionService.ReceiptAmountDecision amountDecision,
            String reviewCombinedRawText
    ) {
    }
}
