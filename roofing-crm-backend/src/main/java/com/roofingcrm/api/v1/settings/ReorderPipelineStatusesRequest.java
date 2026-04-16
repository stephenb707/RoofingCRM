package com.roofingcrm.api.v1.settings;

import com.roofingcrm.domain.enums.PipelineType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ReorderPipelineStatusesRequest {

    @NotNull
    private PipelineType pipelineType;

    @NotEmpty
    private List<UUID> orderedDefinitionIds;
}
