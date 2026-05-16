package com.roofingcrm.service.attachment;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Known-good minimal binaries for upload / validation tests. */
public final class UploadValidationTestFixtures {

    /** 1×1 transparent PNG (decodable by ImageIO). */
    public static final byte[] MINIMAL_PNG_BYTES = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    /** Tiny JPEG (decodable by ImageIO). */
    public static final byte[] MINIMAL_JPEG_BYTES = Base64.getDecoder().decode(
            "/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwCwAA==");

    /** Enough for PDF magic-byte validation (not a full document). */
    public static final byte[] MINIMAL_PDF_BYTES = "%PDF-1.4\n%\n".getBytes(StandardCharsets.US_ASCII);

    private UploadValidationTestFixtures() {
    }
}
