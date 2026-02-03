package com.roofingcrm.api.v1.invoice;

import com.roofingcrm.domain.enums.InvoiceStatus;
import com.roofingcrm.security.SecurityUtils;
import com.roofingcrm.service.invoice.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
@Validated
public class InvoiceController {

    private final InvoiceService invoiceService;

    @Autowired
    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping
    public ResponseEntity<InvoiceDto> createFromEstimate(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @Valid @RequestBody CreateInvoiceFromEstimateRequest request) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        InvoiceDto created = invoiceService.createFromEstimate(tenantId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<Page<InvoiceDto>> listInvoices(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @RequestParam(value = "jobId", required = false) UUID jobId,
            @RequestParam(value = "status", required = false) InvoiceStatus status,
            @PageableDefault(size = 20) Pageable pageable) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        Page<InvoiceDto> page = invoiceService.listInvoices(tenantId, userId, jobId, status, Objects.requireNonNull(pageable));
        return ResponseEntity.ok(page);
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<InvoiceDto>> listInvoicesForJob(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("jobId") UUID jobId) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        List<InvoiceDto> invoices = invoiceService.listInvoicesForJob(tenantId, userId, jobId);
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}")
    public ResponseEntity<InvoiceDto> getInvoice(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("id") UUID invoiceId) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        InvoiceDto dto = invoiceService.getInvoice(tenantId, userId, invoiceId);
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}}/status")
    public ResponseEntity<InvoiceDto> updateStatus(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("id") UUID invoiceId,
            @Valid @RequestBody UpdateInvoiceStatusRequest request) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        InvoiceDto updated = invoiceService.updateStatus(tenantId, userId, invoiceId, request.getStatus());
        return ResponseEntity.ok(updated);
    }
}
