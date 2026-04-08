package com.roofingcrm.service.accounting;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.ai.receipt-ocr")
public class ReceiptOcrProperties {

    private boolean enabled = false;
    private String provider = "tesseract";
    /**
     * Path to the tessdata directory containing traineddata files (e.g. eng.traineddata).
     * If blank, Tesseract uses TESSDATA_PREFIX / default search paths.
     */
    private String tessdataPath = "";
    private String language = "eng";
    /**
     * When false (default), OCR length never suppresses OpenAI vision — vision stays primary for totals.
     * When true, full/summary vision may be skipped only if OCR quality is HIGH and text length meets the min-* thresholds.
     */
    private boolean allowOcrTextLengthToSkipVision = false;
    private int minFullTextCharsToSkipFullVision = 80;
    private int minSummaryTextCharsToSkipSummaryVision = 40;
    private int minCharsForUsableOcr = 20;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getTessdataPath() {
        return tessdataPath;
    }

    public void setTessdataPath(String tessdataPath) {
        this.tessdataPath = tessdataPath;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isAllowOcrTextLengthToSkipVision() {
        return allowOcrTextLengthToSkipVision;
    }

    public void setAllowOcrTextLengthToSkipVision(boolean allowOcrTextLengthToSkipVision) {
        this.allowOcrTextLengthToSkipVision = allowOcrTextLengthToSkipVision;
    }

    public int getMinFullTextCharsToSkipFullVision() {
        return minFullTextCharsToSkipFullVision;
    }

    public void setMinFullTextCharsToSkipFullVision(int minFullTextCharsToSkipFullVision) {
        this.minFullTextCharsToSkipFullVision = minFullTextCharsToSkipFullVision;
    }

    public int getMinSummaryTextCharsToSkipSummaryVision() {
        return minSummaryTextCharsToSkipSummaryVision;
    }

    public void setMinSummaryTextCharsToSkipSummaryVision(int minSummaryTextCharsToSkipSummaryVision) {
        this.minSummaryTextCharsToSkipSummaryVision = minSummaryTextCharsToSkipSummaryVision;
    }

    public int getMinCharsForUsableOcr() {
        return minCharsForUsableOcr;
    }

    public void setMinCharsForUsableOcr(int minCharsForUsableOcr) {
        this.minCharsForUsableOcr = minCharsForUsableOcr;
    }
}
