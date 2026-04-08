package com.roofingcrm.service.accounting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roofingcrm.domain.enums.JobCostCategory;
import org.springframework.http.HttpStatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class OpenAiReceiptExtractionClient implements ReceiptExtractionClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiReceiptExtractionClient.class);

    private static final String SYSTEM_PROMPT = """
            You extract receipt details for a roofing CRM.
            Return only JSON with these keys:
            vendorName: string or null
            incurredDate: string in YYYY-MM-DD format or null
            subtotal: number or null
            tax: number or null
            total: number or null
            amountPaid: number or null
            suggestedAmount: number or null
            suggestedCategory: one of MATERIAL, TRANSPORTATION, LABOR, OTHER, or null
            notes: string or null
            confidence: integer 0-100 or null
            rawExtractedText: string or null

            Rules:
            - Review-first workflow: do not invent values.
            - Use null when the receipt is unclear.
            - Prefer exact labeled summary fields over guesses.
            - Look specifically for labels like SUBTOTAL, TAX, TAX TOTAL, TOTAL, GRAND TOTAL, AMOUNT PAID, AMOUNT DUE, TOTAL DUE, and BALANCE DUE.
            - Focus on the lower summary section of the receipt when extracting amounts.
            - If a label/value pair is unclear, return null rather than guessing.
            - rawExtractedText should be a concise transcription or excerpt of the visible receipt text, prioritizing vendor, date, and the summary/totals lines with their amounts (e.g. Subtotal …, Tax …, Total …), not a single label alone.
            - suggestedAmount is only a fallback best guess and should prefer the final paid total when clearly visible.
            - Category is only a suggestion, not a fact.
            - Keep notes concise and factual.
            - Never include markdown or extra text outside the JSON object.
            """;

    private static final String INTERPRETATION_SYSTEM_PROMPT = """
            You interpret transcribed text from a receipt (e.g. embedded PDF text). Return only JSON with these keys:
            vendorName: string or null
            incurredDate: string in YYYY-MM-DD format or null
            suggestedCategory: one of MATERIAL, TRANSPORTATION, LABOR, OTHER, or null
            notes: string or null
            confidence: integer 0-100 or null

            Rules:
            - Do NOT include any numeric money fields (no subtotal, tax, total, amount paid, suggested amount).
            - Prefer null over guessing.
            - Use only the provided text.
            - Never include markdown or extra text outside the JSON object.
            """;

    private static final String SUMMARY_SYSTEM_PROMPT = """
            You extract only receipt summary totals from a cropped summary region (totals block).
            Return only JSON with these keys:
            subtotal: number or null
            tax: number or null
            total: number or null
            amountPaid: number or null
            confidence: integer 0-100 or null
            rawExtractedText: string or null

            How to read the image:
            - Read this cropped region from top to bottom, line by line, like a strict totals-block reader.
            - When you see a label and a money value on the same line (or label above its value), map that value to the correct JSON field.
            - Look for these labels if visible (case-insensitive): Subtotal, Tax, Tax Total, Total, Grand Total, Amount Paid, Amount Due, Balance Due.
            - Preserve decimal digits exactly (e.g. 1564.38); do not round or drop cents.
            - If a line shows both a label and a numeric value, return that exact numeric value for the matching field.
            - Do not infer amounts from body text, line items, or quantities outside this crop.
            - If only one or two summary lines are visible, fill only those fields and use null for the rest.
            - Return null when a label or its value is not clearly readable in this crop.

            rawExtractedText:
            - Transcribe the visible summary lines as faithfully as possible (each line can look like "Label ... $123.45" or similar).
            - This field is for audit/debug: include enough lines to show you saw Subtotal/Tax/Total/Amount Paid when present, not a single word like "Subtotal" alone.

            Never include markdown or extra text outside the JSON object.
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final int maxRetries;

    public OpenAiReceiptExtractionClient(RestClient.Builder restClientBuilder,
                                         ObjectMapper objectMapper,
                                         String baseUrl,
                                         String apiKey,
                                         String model,
                                         int connectTimeoutSeconds,
                                         int readTimeoutSeconds,
                                         int maxRetries) {
        this(
                restClientBuilder
                        .baseUrl(Objects.requireNonNull(baseUrl))
                        .requestFactory(Objects.requireNonNull(createRequestFactory(connectTimeoutSeconds, readTimeoutSeconds)))
                        .build(),
                objectMapper,
                apiKey,
                model,
                maxRetries
        );
    }

    OpenAiReceiptExtractionClient(RestClient restClient,
                                  ObjectMapper objectMapper,
                                  String apiKey,
                                  String model,
                                  int maxRetries) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.maxRetries = Math.max(0, maxRetries);
    }

    @Override
    public ExtractedReceiptData extract(ReceiptVisionDocument document) {
        return executeExtraction(document, SYSTEM_PROMPT, buildUserPrompt(document));
    }

    @Override
    public ExtractedReceiptData extractSummary(ReceiptVisionDocument document) {
        return executeExtraction(document, SUMMARY_SYSTEM_PROMPT, buildSummaryPrompt(document));
    }

    @Override
    public ExtractedReceiptData interpretFromTranscribedText(
            String fullTranscribedText,
            String summaryTranscribedText,
            ReceiptTextInterpretationContext context) {
        return executeInterpretation(fullTranscribedText, summaryTranscribedText, context);
    }

    private ExtractedReceiptData executeInterpretation(String fullTranscribedText,
                                                       String summaryTranscribedText,
                                                       ReceiptTextInterpretationContext context) {
        String userPrompt = buildInterpretationUserPrompt(fullTranscribedText, summaryTranscribedText, context);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("temperature", 0);
        payload.put("response_format", Map.of("type", "json_object"));
        payload.put("messages", List.of(
                Map.of("role", "system", "content", INTERPRETATION_SYSTEM_PROMPT),
                Map.of("role", "user", "content", userPrompt)
        ));

        int totalAttempts = maxRetries + 1;
        for (int attempt = 1; attempt <= totalAttempts; attempt++) {
            try {
                OpenAiChatCompletionResponse response = restClient.post()
                        .uri("/v1/chat/completions")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .header("Authorization", "Bearer " + apiKey)
                        .body(Objects.requireNonNull(payload))
                        .retrieve()
                        .body(OpenAiChatCompletionResponse.class);
                String content = response != null
                        && response.choices() != null
                        && !response.choices().isEmpty()
                        && response.choices().getFirst().message() != null
                        ? response.choices().getFirst().message().content()
                        : null;
                if (content == null || content.isBlank()) {
                    throw new IllegalStateException("Receipt interpretation provider returned an empty response.");
                }
                OpenAiInterpretationJson parsed = objectMapper.readValue(content, OpenAiInterpretationJson.class);
                return new ExtractedReceiptData(
                        blankToNull(parsed.vendorName()),
                        blankToNull(parsed.incurredDate()),
                        null,
                        null,
                        null,
                        null,
                        null,
                        parsed.suggestedCategory(),
                        blankToNull(parsed.notes()),
                        parsed.confidence(),
                        null
                );
            } catch (RestClientResponseException ex) {
                boolean retryable = isRetryableStatus(ex.getStatusCode());
                log.warn("OpenAI receipt interpretation failed attempt {}/{} with status {}, body={}",
                        attempt,
                        totalAttempts,
                        ex.getStatusCode().value(),
                        abbreviate(ex.getResponseBodyAsString()),
                        ex);
                if (retryable && attempt < totalAttempts) {
                    continue;
                }
                throw new ReceiptExtractionProviderException(
                        "Receipt interpretation provider request failed with status " + ex.getStatusCode().value() + ".", ex);
            } catch (ResourceAccessException ex) {
                log.warn("OpenAI receipt interpretation transport failure attempt {}/{}",
                        attempt,
                        totalAttempts,
                        ex);
                if (attempt < totalAttempts) {
                    continue;
                }
                throw new ReceiptExtractionProviderException(
                        "Receipt interpretation provider request timed out or failed.", ex);
            } catch (RestClientException | IOException ex) {
                log.warn("Receipt interpretation request failed", ex);
                throw new ReceiptExtractionProviderException(
                        "Receipt interpretation provider request failed.", ex);
            }
        }
        throw new ReceiptExtractionProviderException("Receipt interpretation provider request failed.");
    }

    private static String buildInterpretationUserPrompt(String fullTranscribedText,
                                                        String summaryTranscribedText,
                                                        ReceiptTextInterpretationContext context) {
        return """
                Interpret vendor and metadata from this text. Do not include money amounts in your reasoning output.
                File name: %s
                Content type: %s
                Context: %s

                Full text:
                %s

                Summary-region text:
                %s
                """.formatted(
                nullSafe(context.fileName()),
                nullSafe(context.contentType()),
                nullSafe(context.promptContext()),
                nullSafeBlock(fullTranscribedText),
                nullSafeBlock(summaryTranscribedText)
        );
    }

    private static String nullSafeBlock(String value) {
        if (value == null || value.isBlank()) {
            return "(none)";
        }
        String trimmed = value.trim();
        return trimmed.length() <= 12000 ? trimmed : trimmed.substring(0, 12000) + "\n...(truncated)";
    }

    private ExtractedReceiptData executeExtraction(ReceiptVisionDocument document,
                                                   String systemPrompt,
                                                   String userPrompt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("temperature", 0);
        payload.put("response_format", Map.of("type", "json_object"));
        payload.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", List.of(
                        Map.of("type", "text", "text", userPrompt),
                        Map.of(
                                "type", "image_url",
                                "image_url", Map.of(
                                        "url", "data:" + document.imageMimeType() + ";base64," + document.imageBase64(),
                                        "detail", "high"
                                ))
                ))
        ));

        int totalAttempts = maxRetries + 1;
        for (int attempt = 1; attempt <= totalAttempts; attempt++) {
            try {
                OpenAiChatCompletionResponse response = restClient.post()
                        .uri("/v1/chat/completions")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .header("Authorization", "Bearer " + apiKey)
                        .body(Objects.requireNonNull(payload))
                        .retrieve()
                        .body(OpenAiChatCompletionResponse.class);
                String content = response != null
                        && response.choices() != null
                        && !response.choices().isEmpty()
                        && response.choices().getFirst().message() != null
                        ? response.choices().getFirst().message().content()
                        : null;
                if (content == null || content.isBlank()) {
                    throw new IllegalStateException("Receipt extraction provider returned an empty response.");
                }
                OpenAiExtractionJson parsed = objectMapper.readValue(content, OpenAiExtractionJson.class);
                return new ExtractedReceiptData(
                        blankToNull(parsed.vendorName()),
                        blankToNull(parsed.incurredDate()),
                        parsed.subtotal(),
                        parsed.tax(),
                        parsed.total(),
                        parsed.amountPaid(),
                        parsed.suggestedAmount(),
                        parsed.suggestedCategory(),
                        blankToNull(parsed.notes()),
                        parsed.confidence(),
                        blankToNull(parsed.rawExtractedText())
                );
            } catch (RestClientResponseException ex) {
                boolean retryable = isRetryableStatus(ex.getStatusCode());
                log.warn("OpenAI receipt extraction failed for {} attempt {}/{} with status {}, dims={}x{}, bytes={}, body={}",
                        document.attemptLabel(),
                        attempt,
                        totalAttempts,
                        ex.getStatusCode().value(),
                        document.width(),
                        document.height(),
                        document.imageByteSize(),
                        abbreviate(ex.getResponseBodyAsString()),
                        ex);
                if (retryable && attempt < totalAttempts) {
                    continue;
                }
                throw new ReceiptExtractionProviderException(
                        "Receipt extraction provider request failed for " + document.attemptLabel()
                                + " with status " + ex.getStatusCode().value() + ".", ex);
            } catch (ResourceAccessException ex) {
                log.warn("OpenAI receipt extraction transport failure for {} attempt {}/{} dims={}x{}, bytes={}",
                        document.attemptLabel(),
                        attempt,
                        totalAttempts,
                        document.width(),
                        document.height(),
                        document.imageByteSize(),
                        ex);
                if (attempt < totalAttempts) {
                    continue;
                }
                throw new ReceiptExtractionProviderException(
                        "Receipt extraction provider request timed out or failed for " + document.attemptLabel() + ".", ex);
            } catch (RestClientException | IOException ex) {
                log.warn("Receipt extraction request failed for {} dims={}x{}, bytes={}",
                        document.attemptLabel(),
                        document.width(),
                        document.height(),
                        document.imageByteSize(),
                        ex);
                throw new ReceiptExtractionProviderException(
                        "Receipt extraction provider request failed for " + document.attemptLabel() + ".", ex);
            }
        }
        throw new ReceiptExtractionProviderException("Receipt extraction provider request failed for " + document.attemptLabel() + ".");
    }

    private String buildUserPrompt(ReceiptVisionDocument document) {
        return """
                Extract receipt details from this document.
                File name: %s
                Content type: %s
                Existing user label: %s
                Image dimensions: %sx%s
                Prioritize the final summary section and return exact labeled amounts for subtotal, tax, total, and amount paid when visible.
                Do not confuse line-item amounts or subtotal values with the final total.
                """.formatted(
                nullSafe(document.fileName()),
                nullSafe(document.contentType()),
                nullSafe(document.promptContext()),
                nullSafe(document.width()),
                nullSafe(document.height())
        );
    }

    private String buildSummaryPrompt(ReceiptVisionDocument document) {
        return """
                This image is ONLY the bottom summary / totals section of a receipt (cropped).
                Read each visible line in order. Extract subtotal, tax, total, and amount paid only from labeled summary lines in this crop.
                File name: %s
                Content type: %s
                Existing user label: %s
                Crop dimensions: %sx%s
                In rawExtractedText, copy the summary lines you can read (e.g. Subtotal …, Tax …, Total …) so each important label appears with its amount.
                Return null for any field whose label or number is not clearly visible here—do not guess from other parts of the receipt.
                """.formatted(
                nullSafe(document.fileName()),
                nullSafe(document.contentType()),
                nullSafe(document.promptContext()),
                nullSafe(document.width()),
                nullSafe(document.height())
        );
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String nullSafe(String value) {
        return value == null || value.isBlank() ? "none" : value.trim();
    }

    private static String nullSafe(Integer value) {
        return value == null ? "unknown" : Integer.toString(value);
    }

    private static JdkClientHttpRequestFactory createRequestFactory(int connectTimeoutSeconds, int readTimeoutSeconds) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(1, connectTimeoutSeconds)))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(Objects.requireNonNull(httpClient));
        requestFactory.setReadTimeout(Objects.requireNonNull(Duration.ofSeconds(Math.max(1, readTimeoutSeconds))));
        return requestFactory;
    }

    private static boolean isRetryableStatus(HttpStatusCode statusCode) {
        int value = statusCode.value();
        return value >= 500 && value < 600;
    }

    private static String abbreviate(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= 300 ? trimmed : trimmed.substring(0, 300) + "...";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiChatCompletionResponse(List<Choice> choices) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        private record Choice(Message message) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        private record Message(String content) {
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiExtractionJson(
            String vendorName,
            String incurredDate,
            BigDecimal subtotal,
            BigDecimal tax,
            BigDecimal total,
            BigDecimal amountPaid,
            BigDecimal suggestedAmount,
            JobCostCategory suggestedCategory,
            String notes,
            Integer confidence,
            String rawExtractedText
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiInterpretationJson(
            String vendorName,
            String incurredDate,
            JobCostCategory suggestedCategory,
            String notes,
            Integer confidence
    ) {
    }
}
