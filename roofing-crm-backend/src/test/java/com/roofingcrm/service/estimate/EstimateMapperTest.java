package com.roofingcrm.service.estimate;

import com.roofingcrm.domain.entity.Estimate;
import com.roofingcrm.domain.entity.EstimateItem;
import com.roofingcrm.domain.enums.EstimateStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EstimateMapperTest {

    private final EstimateMapper mapper = new EstimateMapper();

    @Test
    void toDto_withNullItems_usesPersistedTotalsAndNoItems() {
        Estimate estimate = new Estimate();
        estimate.setId(UUID.randomUUID());
        estimate.setStatus(EstimateStatus.DRAFT);
        estimate.setTitle("Estimate A");
        estimate.setSubtotal(new BigDecimal("500.00"));
        estimate.setTotal(new BigDecimal("540.00"));
        estimate.setItems(null);

        var dto = mapper.toDto(estimate);

        assertNotNull(dto);
        assertEquals("Estimate A", dto.getTitle());
        assertNull(dto.getItems());
        assertEquals(new BigDecimal("500.00"), dto.getSubtotal());
        assertEquals(new BigDecimal("540.00"), dto.getTotal());
    }

    @Test
    void toDto_withInitializedItems_computesSubtotalWhenMissing() {
        Estimate estimate = new Estimate();
        estimate.setId(UUID.randomUUID());
        estimate.setStatus(EstimateStatus.SENT);
        estimate.setSubtotal(null);
        estimate.setTotal(null);

        EstimateItem item1 = new EstimateItem();
        item1.setId(UUID.randomUUID());
        item1.setName("Shingles");
        item1.setQuantity(new BigDecimal("2"));
        item1.setUnitPrice(new BigDecimal("100.00"));
        item1.setLineTotal(new BigDecimal("200.00"));

        EstimateItem item2 = new EstimateItem();
        item2.setId(UUID.randomUUID());
        item2.setName("Labor");
        item2.setQuantity(new BigDecimal("1"));
        item2.setUnitPrice(new BigDecimal("50.00"));
        item2.setLineTotal(new BigDecimal("50.00"));

        estimate.setItems(new ArrayList<>(List.of(item1, item2)));

        var dto = mapper.toDto(estimate);

        assertNotNull(dto.getItems());
        assertEquals(2, dto.getItems().size());
        assertEquals(new BigDecimal("250.00"), dto.getSubtotal());
        assertEquals(new BigDecimal("250.00"), dto.getTotal());
    }
}
