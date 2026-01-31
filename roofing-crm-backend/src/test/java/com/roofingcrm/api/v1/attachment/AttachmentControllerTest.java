package com.roofingcrm.api.v1.attachment;

import com.roofingcrm.security.AuthenticatedUser;
import com.roofingcrm.service.attachment.AttachmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AttachmentController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class AttachmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AttachmentService attachmentService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        AuthenticatedUser authUser = new AuthenticatedUser(Objects.requireNonNull(userId), "test@example.com");
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(authUser, null);
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void uploadForLead_returnsCreated() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID leadId = UUID.randomUUID();

        AttachmentDto responseDto = new AttachmentDto();
        responseDto.setId(UUID.randomUUID());
        responseDto.setFileName("test-doc.pdf");
        responseDto.setContentType("application/pdf");
        responseDto.setFileSize(1024L);
        responseDto.setStorageProvider("LOCAL");
        responseDto.setLeadId(leadId);
        responseDto.setCreatedAt(Instant.now());

        when(attachmentService.uploadForLead(eq(Objects.requireNonNull(tenantId)), eq(Objects.requireNonNull(userId)), eq(Objects.requireNonNull(leadId)), any(), any(), any()))
                .thenReturn(responseDto);

        MockMultipartFile file = new MockMultipartFile(
                "file", "test-doc.pdf", "application/pdf", "Test content".getBytes());

        mockMvc.perform(multipart("/api/v1/leads/{leadId}/attachments", leadId)
                        .file(file)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName", Objects.requireNonNull(is("test-doc.pdf"))))
                .andExpect(jsonPath("$.storageProvider", Objects.requireNonNull(is("LOCAL"))));
    }

    @Test
    void uploadForJob_returnsCreated() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        AttachmentDto responseDto = new AttachmentDto();
        responseDto.setId(UUID.randomUUID());
        responseDto.setFileName("job-photo.jpg");
        responseDto.setContentType("image/jpeg");
        responseDto.setFileSize(2048L);
        responseDto.setStorageProvider("LOCAL");
        responseDto.setJobId(jobId);
        responseDto.setCreatedAt(Instant.now());

        when(attachmentService.uploadForJob(eq(Objects.requireNonNull(tenantId)), eq(Objects.requireNonNull(userId)), eq(Objects.requireNonNull(jobId)), any(), any(), any()))
                .thenReturn(responseDto);

        MockMultipartFile file = new MockMultipartFile(
                "file", "job-photo.jpg", "image/jpeg", "Fake image".getBytes());

        mockMvc.perform(multipart("/api/v1/jobs/{jobId}/attachments", jobId)
                        .file(file)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName", Objects.requireNonNull(is("job-photo.jpg"))));
    }

    @Test
    void listForLead_returnsOk() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID leadId = UUID.randomUUID();

        AttachmentDto dto1 = new AttachmentDto();
        dto1.setId(UUID.randomUUID());
        dto1.setFileName("doc1.pdf");
        dto1.setLeadId(leadId);

        AttachmentDto dto2 = new AttachmentDto();
        dto2.setId(UUID.randomUUID());
        dto2.setFileName("doc2.pdf");
        dto2.setLeadId(leadId);

        when(attachmentService.listForLead(eq(Objects.requireNonNull(tenantId)), eq(Objects.requireNonNull(userId)), eq(Objects.requireNonNull(leadId))))
                .thenReturn(List.of(dto1, dto2));

        mockMvc.perform(get("/api/v1/leads/{leadId}/attachments", leadId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Objects.requireNonNull(hasSize(2))))
                .andExpect(jsonPath("$[0].fileName", Objects.requireNonNull(is("doc1.pdf"))));
    }

    @Test
    void downloadAttachment_returnsFile() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();

        AttachmentDto metadata = new AttachmentDto();
        metadata.setId(attachmentId);
        metadata.setFileName("download.pdf");
        metadata.setContentType("application/pdf");

        when(attachmentService.getAttachment(eq(Objects.requireNonNull(tenantId)), eq(Objects.requireNonNull(userId)), eq(Objects.requireNonNull(attachmentId))))
                .thenReturn(metadata);

        byte[] fileContent = "PDF file content".getBytes();
        when(attachmentService.loadAttachmentContent(eq(Objects.requireNonNull(tenantId)), eq(Objects.requireNonNull(userId)), eq(Objects.requireNonNull(attachmentId))))
                .thenReturn(new ByteArrayInputStream(fileContent));

        mockMvc.perform(get("/api/v1/attachments/{id}/download", attachmentId)
                        .header("X-Tenant-Id", tenantId.toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"download.pdf\""));
    }
}
