package com.roofingcrm.service.estimate;

import com.roofingcrm.domain.entity.Estimate;
import com.roofingcrm.domain.repository.EstimateRepository;
import com.roofingcrm.security.PublicShareTokenHasher;
import com.roofingcrm.service.activity.ActivityEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class PublicEstimateServiceUnitTest {

    @Mock
    private EstimateRepository estimateRepository;

    @Mock
    private ActivityEventService activityEventService;

    private PublicEstimateService service;

    @BeforeEach
    void setUp() {
        service = new PublicEstimateService(estimateRepository, activityEventService);
    }

    @Test
    void getByToken_looksUpBySha256Hash() {
        String raw = "customer-facing-token";
        String hash = PublicShareTokenHasher.sha256HexUtf8(raw);
        Estimate estimate = new Estimate();
        estimate.setId(UUID.randomUUID());
        when(estimateRepository.findByPublicTokenHashAndPublicEnabledTrueAndArchivedFalse(eq(hash)))
                .thenReturn(Optional.of(estimate));

        service.getByToken(raw);

        verify(estimateRepository).findByPublicTokenHashAndPublicEnabledTrueAndArchivedFalse(eq(hash));
    }
}
