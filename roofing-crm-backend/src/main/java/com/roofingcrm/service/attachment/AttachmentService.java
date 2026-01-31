package com.roofingcrm.service.attachment;

import com.roofingcrm.api.v1.attachment.AttachmentDto;
import com.roofingcrm.domain.enums.AttachmentTag;
import org.springframework.lang.NonNull;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public interface AttachmentService {

    AttachmentDto uploadForLead(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull UUID leadId, MultipartFile file,
                                AttachmentTag tag, String description);

    AttachmentDto uploadForJob(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull UUID jobId, MultipartFile file,
                               AttachmentTag tag, String description);

    List<AttachmentDto> listForLead(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull UUID leadId);

    List<AttachmentDto> listForJob(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull UUID jobId);

    AttachmentDto getAttachment(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull UUID attachmentId);

    InputStream loadAttachmentContent(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull UUID attachmentId);
}
