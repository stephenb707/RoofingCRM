package com.roofingcrm.service.invoice;

import com.roofingcrm.api.v1.invoice.CreateInvoiceFromEstimateRequest;
import com.roofingcrm.api.v1.invoice.InvoiceDto;
import com.roofingcrm.api.v1.invoice.InvoiceSummaryDto;
import com.roofingcrm.api.v1.invoice.ShareInvoiceRequest;
import com.roofingcrm.api.v1.invoice.ShareInvoiceResponse;
import com.roofingcrm.domain.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.UUID;

public interface InvoiceService {

    InvoiceDto createFromEstimate(@NonNull UUID tenantId, @NonNull UUID userId, CreateInvoiceFromEstimateRequest request);

    Page<InvoiceSummaryDto> listInvoices(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId, InvoiceStatus status, @NonNull Pageable pageable);

    List<InvoiceSummaryDto> listInvoicesForJob(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId);

    InvoiceDto getInvoice(@NonNull UUID tenantId, @NonNull UUID userId, UUID invoiceId);

    InvoiceDto updateStatus(@NonNull UUID tenantId, @NonNull UUID userId, UUID invoiceId, InvoiceStatus newStatus);

    ShareInvoiceResponse shareInvoice(@NonNull UUID tenantId, @NonNull UUID userId, UUID invoiceId, ShareInvoiceRequest request);
}
