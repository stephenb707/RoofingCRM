package com.roofingcrm.storage;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class S3AttachmentStorageServiceTest {

    @Test
    void store_invokesPutObjectWithExpectedKey() {
        S3Client s3 = mock(S3Client.class);
        S3AttachmentStorageProperties props = new S3AttachmentStorageProperties();
        props.setBucket("my-bucket");
        props.setPrefix("app");

        UUID tenant = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID att = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        S3AttachmentStorageService svc = new S3AttachmentStorageService(s3, props);

        MockMultipartFile file = new MockMultipartFile("f", "doc.png", "image/png", "x".getBytes(StandardCharsets.UTF_8));
        String key = svc.store(tenant, "ignored-slug", att, file);

        assertTrue(key.startsWith("app/tenants/" + tenant + "/attachments/" + att + "/"));

        ArgumentCaptor<PutObjectRequest> putCap = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(putCap.capture(), any(RequestBody.class));
        assertEquals("my-bucket", putCap.getValue().bucket());
        assertEquals(key, putCap.getValue().key());
        assertEquals("image/png", putCap.getValue().contentType());
    }

    @Test
    @SuppressWarnings("unchecked")
    void loadAsStream_invokesGetObjectForKey() {
        S3Client s3 = mock(S3Client.class);
        ResponseInputStream<GetObjectResponse> body = mock(ResponseInputStream.class);
        when(s3.getObject(any(GetObjectRequest.class))).thenReturn(body);
        S3AttachmentStorageProperties props = new S3AttachmentStorageProperties();
        props.setBucket("b");
        props.setPrefix("");
        UUID tenant = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        UUID att = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        String key = S3ObjectKeyBuilder.buildObjectKey(tenant, att, "doc.pdf", "");

        S3AttachmentStorageService svc = new S3AttachmentStorageService(s3, props);
        assertNotNull(svc.loadAsStream(tenant, "any", key));

        ArgumentCaptor<GetObjectRequest> getCap = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3).getObject(getCap.capture());
        assertEquals("b", getCap.getValue().bucket());
        assertEquals(key, getCap.getValue().key());
    }

    @Test
    void loadAsStream_rejectsWrongTenant() {
        S3Client s3 = mock(S3Client.class);
        S3AttachmentStorageProperties props = new S3AttachmentStorageProperties();
        props.setBucket("b");
        S3AttachmentStorageService svc = new S3AttachmentStorageService(s3, props);
        UUID t1 = UUID.randomUUID();
        UUID t2 = UUID.randomUUID();
        String keyForOther = S3ObjectKeyBuilder.buildObjectKey(t2, UUID.randomUUID(), "a.bin", "");
        assertThrows(IllegalArgumentException.class, () -> svc.loadAsStream(t1, "s", keyForOther));
        verifyNoInteractions(s3);
    }
}
