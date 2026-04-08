package com.roofingcrm.service.accounting;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.mock;

class ReceiptExtractionConfigTest {

    private final ReceiptExtractionConfig config = new ReceiptExtractionConfig();

    @Test
    void receiptExtractionClient_whenEnabledWithOpenAiConfig_returnsOpenAiClient() {
        ReceiptExtractionProperties properties = new ReceiptExtractionProperties();
        properties.setEnabled(true);
        properties.setProvider(" OPENAI ");
        properties.getOpenai().setApiKey("test-key");
        properties.getOpenai().setModel(" gpt-4o-mini ");

        ReceiptExtractionClient client = config.receiptExtractionClient(
                mock(RestClient.Builder.class, RETURNS_SELF),
                new ObjectMapper(),
                properties
        );

        assertInstanceOf(OpenAiReceiptExtractionClient.class, client);
    }

    @Test
    void receiptExtractionClient_whenDisabled_returnsNoopClient() {
        ReceiptExtractionProperties properties = new ReceiptExtractionProperties();

        ReceiptExtractionClient client = config.receiptExtractionClient(
                mock(RestClient.Builder.class, RETURNS_SELF),
                new ObjectMapper(),
                properties
        );

        assertInstanceOf(NoopReceiptExtractionClient.class, client);
    }

    @Test
    void receiptExtractionClient_whenApiKeyMissing_returnsNoopClient() {
        ReceiptExtractionProperties properties = new ReceiptExtractionProperties();
        properties.setEnabled(true);
        properties.setProvider("openai");

        ReceiptExtractionClient client = config.receiptExtractionClient(
                mock(RestClient.Builder.class, RETURNS_SELF),
                new ObjectMapper(),
                properties
        );

        assertInstanceOf(NoopReceiptExtractionClient.class, client);
    }
}
