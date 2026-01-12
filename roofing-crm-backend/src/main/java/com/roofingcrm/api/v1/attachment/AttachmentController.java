package com.roofingcrm.api.v1.attachment;

import com.roofingcrm.security.SecurityUtils;
import com.roofingcrm.service.attachment.AttachmentService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Validated
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping("/leads/{leadId}/attachments")
    public ResponseEntity<AttachmentDto> uploadForLead(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("leadId") @NonNull UUID leadId,
            @RequestParam("file") MultipartFile file) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        AttachmentDto dto = attachmentService.uploadForLead(tenantId, userId, leadId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/jobs/{jobId}/attachments")
    public ResponseEntity<AttachmentDto> uploadForJob(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("jobId") @NonNull UUID jobId,
            @RequestParam("file") MultipartFile file) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        AttachmentDto dto = attachmentService.uploadForJob(tenantId, userId, jobId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/leads/{leadId}/attachments")
    public ResponseEntity<List<AttachmentDto>> listForLead(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("leadId") @NonNull UUID leadId) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        List<AttachmentDto> attachments = attachmentService.listForLead(tenantId, userId, leadId);
        return ResponseEntity.ok(attachments);
    }

    @GetMapping("/jobs/{jobId}/attachments")
    public ResponseEntity<List<AttachmentDto>> listForJob(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("jobId") @NonNull UUID jobId) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        List<AttachmentDto> attachments = attachmentService.listForJob(tenantId, userId, jobId);
        return ResponseEntity.ok(attachments);
    }

    @GetMapping("/attachments/{id}")
    public ResponseEntity<AttachmentDto> getAttachment(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("id") @NonNull UUID attachmentId) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        AttachmentDto dto = attachmentService.getAttachment(tenantId, userId, attachmentId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/attachments/{id}/download")
    public ResponseEntity<InputStreamResource> downloadAttachment(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("id") @NonNull UUID attachmentId) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        
        // Get metadata first
        AttachmentDto metadata = attachmentService.getAttachment(tenantId, userId, attachmentId);
        
        // Load content stream
        InputStream content = attachmentService.loadAttachmentContent(tenantId, userId, attachmentId);
        
        String contentType = Objects.requireNonNull(metadata.getContentType() != null 
                ? metadata.getContentType() 
                : MediaType.APPLICATION_OCTET_STREAM_VALUE);
        
        String fileName = metadata.getFileName() != null 
                ? metadata.getFileName() 
                : attachmentId.toString();

        return ResponseEntity.ok()
                .contentType(Objects.requireNonNull(MediaType.parseMediaType(contentType)))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(new InputStreamResource(Objects.requireNonNull(content)));
    }
}
