package com.roofingcrm.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.Instant;

@Getter
public class ApiErrorResponse {

    private final Instant timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String correlationId;

    public ApiErrorResponse(Instant timestamp, int status, String error, String message, String path) {
        this(timestamp, status, error, message, path, null);
    }

    public ApiErrorResponse(Instant timestamp, int status, String error, String message, String path,
                            String correlationId) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.correlationId = correlationId;
    }
}