package com.roofingcrm.service.accounting;

public class NoopReceiptExtractionClient implements ReceiptExtractionClient {

    private final String message;

    public NoopReceiptExtractionClient(String message) {
        this.message = message;
    }

    @Override
    public ExtractedReceiptData extract(ReceiptVisionDocument document) {
        throw new ReceiptExtractionUnavailableException(message);
    }

    @Override
    public ExtractedReceiptData extractSummary(ReceiptVisionDocument document) {
        throw new ReceiptExtractionUnavailableException(message);
    }

    @Override
    public ExtractedReceiptData interpretFromTranscribedText(
            String fullOcrText,
            String summaryOcrText,
            ReceiptTextInterpretationContext context) {
        return new ExtractedReceiptData(
                null, null, null, null, null, null, null, null, null, null, null
        );
    }
}
