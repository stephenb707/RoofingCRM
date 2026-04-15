package com.roofingcrm.api.v1.customerreport;

import com.roofingcrm.api.v1.attachment.AttachmentDto;
import com.roofingcrm.security.SecurityUtils;
import com.roofingcrm.service.customerreport.CustomerPhotoReportPdfExport;
import com.roofingcrm.service.customerreport.CustomerPhotoReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer-photo-reports")
@Validated
public class CustomerPhotoReportController {

    private final CustomerPhotoReportService customerPhotoReportService;

    public CustomerPhotoReportController(CustomerPhotoReportService customerPhotoReportService) {
        this.customerPhotoReportService = customerPhotoReportService;
    }

    @GetMapping("/attachment-candidates")
    public List<AttachmentDto> attachmentCandidates(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @RequestParam("customerId") @NonNull UUID customerId,
            @RequestParam(value = "jobId", required = false) UUID jobId) {
        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        return customerPhotoReportService.listAttachmentCandidates(tenantId, userId, customerId, jobId);
    }

    @GetMapping
    public List<CustomerPhotoReportSummaryDto> list(@RequestHeader("X-Tenant-Id") @NonNull UUID tenantId) {
        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        return customerPhotoReportService.list(tenantId, userId);
    }

    @PostMapping
    public CustomerPhotoReportDto create(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @Valid @RequestBody UpsertCustomerPhotoReportRequest request) {
        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        return customerPhotoReportService.create(tenantId, userId, request);
    }

    @GetMapping("/{reportId}")
    public CustomerPhotoReportDto get(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("reportId") UUID reportId) {
        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        return customerPhotoReportService.get(tenantId, userId, reportId);
    }

    @PutMapping("/{reportId}")
    public CustomerPhotoReportDto update(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("reportId") UUID reportId,
            @Valid @RequestBody UpsertCustomerPhotoReportRequest request) {
        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        return customerPhotoReportService.update(tenantId, userId, reportId, request);
    }

    @DeleteMapping("/{reportId}")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("reportId") UUID reportId) {
        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        customerPhotoReportService.archive(tenantId, userId, reportId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{reportId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadPdf(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("reportId") UUID reportId) {
        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        CustomerPhotoReportPdfExport exported = customerPhotoReportService.exportPdf(tenantId, userId, reportId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exported.filename() + "\"")
                .body(exported.content());
    }

    @PostMapping("/{reportId}/send-email")
    public SendCustomerPhotoReportEmailResponse sendEmail(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("reportId") UUID reportId,
            @Valid @RequestBody SendCustomerPhotoReportEmailRequest request) {
        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        return customerPhotoReportService.sendEmail(tenantId, userId, reportId, request);
    }
}
