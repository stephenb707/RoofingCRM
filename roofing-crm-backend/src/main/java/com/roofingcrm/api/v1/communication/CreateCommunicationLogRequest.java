package com.roofingcrm.api.v1.communication;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class CreateCommunicationLogRequest {

    @NotBlank
    private String channel;

    private String direction;

    @NotBlank
    private String subject;

    private String body;

    // Optional; if null, default to now in the service
    private Instant occurredAt;
}
