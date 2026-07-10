package com.roofingcrm.background;

import com.roofingcrm.domain.entity.IntegrationBackgroundJob;
import org.springframework.stereotype.Component;

@Component
public class NoOpBackgroundJobHandler implements BackgroundJobHandler {

    @Override
    public boolean supports(String jobType) {
        return BackgroundJobTypes.NO_OP.equals(jobType);
    }

    @Override
    public void handle(IntegrationBackgroundJob job) {
        // intentionally empty — used for pipeline tests
    }
}
