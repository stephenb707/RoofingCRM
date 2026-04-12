package com.roofingcrm.api.v1.settings;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePipelineStatusRequest {

    @NotBlank
    private String label;
}
