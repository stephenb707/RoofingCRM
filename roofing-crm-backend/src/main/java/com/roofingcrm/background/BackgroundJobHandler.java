package com.roofingcrm.background;

import com.roofingcrm.domain.entity.IntegrationBackgroundJob;

public interface BackgroundJobHandler {

    boolean supports(String jobType);

    void handle(IntegrationBackgroundJob job) throws Exception;
}
