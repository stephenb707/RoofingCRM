package com.roofingcrm.api.v1.communication;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class CommunicationLogDto {

    private UUID id;

    private String channel;    // CALL, SMS, EMAIL, NOTE
    private String direction;  // INBOUND, OUTBOUND, or null for NOTE
    private String subject;
    private String body;

    private Instant occurredAt;

    private UUID leadId;
    private UUID jobId;

    private Instant createdAt;
    private Instant updatedAt;
}
