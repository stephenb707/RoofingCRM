package com.roofingcrm.service.customerreport;

import com.roofingcrm.api.v1.attachment.AttachmentDto;
import com.roofingcrm.api.v1.customerreport.CustomerPhotoReportDto;
import com.roofingcrm.api.v1.customerreport.CustomerPhotoReportSummaryDto;
import com.roofingcrm.api.v1.customerreport.SendCustomerPhotoReportEmailRequest;
import com.roofingcrm.api.v1.customerreport.SendCustomerPhotoReportEmailResponse;
import com.roofingcrm.api.v1.customerreport.UpsertCustomerPhotoReportRequest;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.UUID;

public interface CustomerPhotoReportService {

    List<CustomerPhotoReportSummaryDto> list(@NonNull UUID tenantId, @NonNull UUID userId);

    CustomerPhotoReportDto get(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull UUID reportId);

    CustomerPhotoReportDto create(@NonNull UUID tenantId, @NonNull UUID userId, UpsertCustomerPhotoReportRequest request);

    CustomerPhotoReportDto update(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull UUID reportId,
                                    UpsertCustomerPhotoReportRequest request);

    void archive(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull UUID reportId);

    CustomerPhotoReportPdfExport exportPdf(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull UUID reportId);

    SendCustomerPhotoReportEmailResponse sendEmail(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull UUID reportId,
                                                   @NonNull SendCustomerPhotoReportEmailRequest request);

    List<AttachmentDto> listAttachmentCandidates(@NonNull UUID tenantId, @NonNull UUID userId,
                                                 @NonNull UUID customerId, UUID jobId);
}
