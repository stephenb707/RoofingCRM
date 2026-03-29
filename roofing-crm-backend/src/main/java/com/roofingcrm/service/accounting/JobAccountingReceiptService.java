package com.roofingcrm.service.accounting;

import com.roofingcrm.api.v1.accounting.CreateCostFromReceiptRequest;
import com.roofingcrm.api.v1.accounting.JobCostEntryDto;
import com.roofingcrm.api.v1.accounting.JobReceiptDto;
import org.springframework.lang.NonNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface JobAccountingReceiptService {

    List<JobReceiptDto> listReceiptsForJob(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId);

    JobReceiptDto uploadReceiptForJob(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId, MultipartFile file, String description);

    JobCostEntryDto createCostFromReceipt(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId, UUID receiptId,
                                          CreateCostFromReceiptRequest request);

    JobReceiptDto linkReceiptToCost(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId, UUID receiptId, UUID costEntryId);

    JobReceiptDto unlinkReceiptFromCost(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId, UUID receiptId);

    void deleteReceipt(@NonNull UUID tenantId, @NonNull UUID userId, UUID jobId, UUID receiptId);
}
