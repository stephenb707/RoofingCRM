package com.roofingcrm.api.v1.settings;

import com.roofingcrm.domain.enums.PipelineType;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class PipelineStatusDefinitionDto {

    private UUID id;
    private PipelineType pipelineType;
    private String systemKey;
    private String label;
    private int sortOrder;
    private boolean builtIn;
    private boolean active;
}
