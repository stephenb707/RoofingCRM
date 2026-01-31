package com.roofingcrm.api.v1.attachment;

import com.roofingcrm.domain.enums.AttachmentTag;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class AttachmentDto {

    private UUID id;

    private String fileName;
    private String contentType;
    private Long fileSize;

    private String storageProvider;
    private String storageKey;

    private UUID leadId;
    private UUID jobId;

    private String description;
    private AttachmentTag tag;

    private Instant createdAt;
    private Instant updatedAt;
}
