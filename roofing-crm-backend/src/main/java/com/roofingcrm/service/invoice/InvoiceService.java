package com.roofingcrm.service.invoice;

import com.roofingcrm.api.v1.invoice.CreateInvoiceFromEstimateRequest;
import com.roofingcrm.api.v1.invoice.InvoiceDto;
import com.roofingcrm.domain.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.UUID;

public interface InvoiceService {

    InvoiceDto createFromEstimate(@NonNull UUID tenantId, @NonNull UUID userId, CreateInvoiceFromEstimateRequest request);

    Page<InvoiceDto> listInvoices(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId, InvoiceStatus status, @NonNull Pageable pageable);

    List<InvoiceDto> listInvoicesForJob(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId);

    InvoiceDto getInvoice(@NonNull UUID tenantId, @NonNull UUID userId, UUID invoiceId);

    InvoiceDto updateStatus(@NonNull UUID tenantId, @NonNull UUID userId, UUID invoiceId, InvoiceStatus newStatus);
}
