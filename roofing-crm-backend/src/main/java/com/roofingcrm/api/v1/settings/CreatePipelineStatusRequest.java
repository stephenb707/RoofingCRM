package com.roofingcrm.api.v1.settings;

import com.roofingcrm.domain.enums.PipelineType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePipelineStatusRequest {

    @NotNull
    private PipelineType pipelineType;

    @NotBlank
    private String label;
}
