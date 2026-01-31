package com.roofingcrm.api.v1.activity;

import com.roofingcrm.domain.enums.ActivityEntityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateNoteRequest {

    @NotNull
    private ActivityEntityType entityType;

    @NotNull
    private UUID entityId;

    @NotBlank
    @Size(max = 5000)
    private String body;
}
