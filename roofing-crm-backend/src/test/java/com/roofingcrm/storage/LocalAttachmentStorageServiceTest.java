package com.roofingcrm.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SuppressWarnings("null")
class LocalAttachmentStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void resolveStrictlyUnderBase_rejectsAbsoluteAndTraversal() {
        Path base = tempDir.resolve("vault").toAbsolutePath().normalize();

        Path nested = LocalAttachmentStorageService.resolveStrictlyUnderBase(base, "tenant/sub/file.txt");
        assertTrue(nested.startsWith(base));

        assertThrows(IllegalArgumentException.class,
                () -> LocalAttachmentStorageService.resolveStrictlyUnderBase(base, "../outside"));
        assertThrows(IllegalArgumentException.class,
                () -> LocalAttachmentStorageService.resolveStrictlyUnderBase(base, ""));
        assertThrows(IllegalArgumentException.class,
                () -> LocalAttachmentStorageService.resolveStrictlyUnderBase(base, "tenant/../../outside"));

        Path root = base.getRoot();
        assumeTrue(root != null);
        String absoluteKey = root.resolve("attachment-abs-key-" + UUID.randomUUID()).toString();
        assertTrue(Path.of(absoluteKey).isAbsolute());
        assertThrows(IllegalArgumentException.class,
                () -> LocalAttachmentStorageService.resolveStrictlyUnderBase(base, absoluteKey));
    }

    @Test
    void store_writesUnderTenantAndSanitizesFilename() throws IOException {
        LocalStorageProperties props = new LocalStorageProperties();
        props.setBaseDir(tempDir.resolve("uploads").toString());
        LocalAttachmentStorageService svc = new LocalAttachmentStorageService(props);

        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "..\\..\\../../../passwd",
                "application/octet-stream",
                "secret".getBytes(StandardCharsets.UTF_8));

        String key = svc.store("tenant-one", id, file);

        assertEquals("tenant-one/" + id + "_passwd", key);
        Path expected = tempDir.resolve("uploads").resolve("tenant-one").resolve(id + "_passwd").normalize();
        assertTrue(Files.exists(expected));
        assertEquals("secret", Files.readString(expected));
    }

    @Test
    void store_rejectsUnsafeTenantSlug() {
        LocalStorageProperties props = new LocalStorageProperties();
        props.setBaseDir(tempDir.resolve("uploads").toString());
        LocalAttachmentStorageService svc = new LocalAttachmentStorageService(props);

        MockMultipartFile file = new MockMultipartFile("file", "a.txt", null, "x".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalArgumentException.class, () -> svc.store("..", UUID.randomUUID(), file));
        assertThrows(IllegalArgumentException.class, () -> svc.store("a/x", UUID.randomUUID(), file));
    }

    @Test
    void loadAsStream_rejectsTraversalInStorageKey() {
        LocalStorageProperties props = new LocalStorageProperties();
        props.setBaseDir(tempDir.resolve("uploads").toString());
        LocalAttachmentStorageService svc = new LocalAttachmentStorageService(props);

        assertThrows(IllegalArgumentException.class, () -> svc.loadAsStream("../secrets"));
    }

    @Test
    void loadAsStream_readsFileUnderBase() throws IOException {
        LocalStorageProperties props = new LocalStorageProperties();
        Path base = Files.createDirectories(tempDir.resolve("uploads"));
        props.setBaseDir(base.toString());
        LocalAttachmentStorageService svc = new LocalAttachmentStorageService(props);

        Path physical = Files.createDirectories(base.resolve("t")).resolve("f.bin");
        Files.writeString(physical, "payload");

        try (var in = svc.loadAsStream("t/f.bin")) {
            assertArrayEquals("payload".getBytes(StandardCharsets.UTF_8), in.readAllBytes());
        }
    }
}
