package com.roofingcrm.service.accounting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenAiReceiptExtractionClientTest {

    @Test
    void executeExtraction_includesImageDetailHighInPayloadForFullImageAndSummary() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        AtomicReference<String> capturedBody = new AtomicReference<>();

        try (TestServer server = new TestServer(exchange -> {
            capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            write(exchange, 200, minimalFullExtractionResponse());
        })) {
            OpenAiReceiptExtractionClient client = new OpenAiReceiptExtractionClient(
                    RestClient.builder().baseUrl(Objects.requireNonNull(server.baseUrl())).build(),
                    mapper,
                    "test-key",
                    "gpt-4o-mini",
                    0
            );
            client.extract(document("full-image"));
            assertImageDetailHigh(mapper, capturedBody.get());
        }

        try (TestServer server = new TestServer(exchange -> {
            capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            write(exchange, 200, minimalSummaryExtractionResponse());
        })) {
            OpenAiReceiptExtractionClient client = new OpenAiReceiptExtractionClient(
                    RestClient.builder().baseUrl(Objects.requireNonNull(server.baseUrl())).build(),
                    mapper,
                    "test-key",
                    "gpt-4o-mini",
                    0
            );
            client.extractSummary(document("summary-tight-baseline"));
            assertImageDetailHigh(mapper, capturedBody.get());
        }
    }

    private static void assertImageDetailHigh(ObjectMapper mapper, String requestBody) throws IOException {
        JsonNode root = mapper.readTree(requestBody);
        JsonNode detail = root.at("/messages/1/content/1/image_url/detail");
        assertEquals("high", detail.asText(), "OpenAI image_url must set detail=high for receipt vision");
        JsonNode url = root.at("/messages/1/content/1/image_url/url");
        assertEquals(true, url.asText().startsWith("data:image/png;base64,"));
    }

    private static String minimalFullExtractionResponse() {
        return """
                {"choices":[{"message":{"content":"{\\"vendorName\\":null,\\"incurredDate\\":null,\\"subtotal\\":null,\\"tax\\":null,\\"total\\":null,\\"amountPaid\\":null,\\"suggestedAmount\\":null,\\"suggestedCategory\\":null,\\"notes\\":null,\\"confidence\\":null,\\"rawExtractedText\\":null}"}}]}
                """;
    }

    private static String minimalSummaryExtractionResponse() {
        return """
                {"choices":[{"message":{"content":"{\\"subtotal\\":null,\\"tax\\":null,\\"total\\":null,\\"amountPaid\\":null,\\"confidence\\":null,\\"rawExtractedText\\":null}"}}]}
                """;
    }

    @Test
    void extractSummary_retriesOnceOnTransientServerFailure() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        try (TestServer server = new TestServer(exchange -> {
            int attempt = requestCount.incrementAndGet();
            if (attempt == 1) {
                write(exchange, 500, "{\"error\":\"temporary\"}");
                return;
            }
            write(exchange, 200, """
                    {"choices":[{"message":{"content":"{\\"subtotal\\":1455.24,\\"tax\\":109.14,\\"total\\":1564.38,\\"amountPaid\\":1564.38,\\"confidence\\":88,\\"rawExtractedText\\":\\"TOTAL 1564.38\\"}"}}]}
                    """);
        })) {
            OpenAiReceiptExtractionClient client = new OpenAiReceiptExtractionClient(
                    RestClient.builder().baseUrl(Objects.requireNonNull(server.baseUrl())).build(),
                    new ObjectMapper(),
                    "test-key",
                    "gpt-4o-mini",
                    1
            );

            ReceiptExtractionClient.ExtractedReceiptData result = client.extractSummary(document("summary-tight-baseline"));

            assertEquals(2, requestCount.get());
            assertEquals(new BigDecimal("1564.38"), result.total());
        }
    }

    @Test
    void extractSummary_doesNotRetryBadRequest() throws Exception {
        AtomicInteger requestCount = new AtomicInteger();
        try (TestServer server = new TestServer(exchange -> {
            requestCount.incrementAndGet();
            write(exchange, 400, "{\"error\":\"bad request\"}");
        })) {
            OpenAiReceiptExtractionClient client = new OpenAiReceiptExtractionClient(
                    RestClient.builder().baseUrl(Objects.requireNonNull(server.baseUrl())).build(),
                    new ObjectMapper(),
                    "test-key",
                    "gpt-4o-mini",
                    1
            );

            assertThrows(ReceiptExtractionProviderException.class,
                    () -> client.extractSummary(document("summary-tight-baseline")));
            assertEquals(1, requestCount.get());
        }
    }

    private static ReceiptExtractionClient.ReceiptVisionDocument document(String attemptLabel) {
        return new ReceiptExtractionClient.ReceiptVisionDocument(
                attemptLabel,
                "receipt.png",
                "image/png",
                attemptLabel,
                "image/png",
                "ZmFrZQ==",
                1200,
                400,
                1024
        );
    }

    private interface Handler {
        void handle(HttpExchange exchange) throws IOException;
    }

    private static final class TestServer implements AutoCloseable {
        private final HttpServer server;

        private TestServer(Handler handler) throws IOException {
            this.server = HttpServer.create(new InetSocketAddress(0), 0);
            this.server.createContext("/v1/chat/completions", exchange -> {
                try {
                    handler.handle(exchange);
                } finally {
                    exchange.close();
                }
            });
            this.server.start();
        }

        private String baseUrl() {
            return "http://localhost:" + server.getAddress().getPort();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }

    private static void write(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}
