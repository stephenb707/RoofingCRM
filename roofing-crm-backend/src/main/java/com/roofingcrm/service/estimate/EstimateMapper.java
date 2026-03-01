package com.roofingcrm.service.estimate;

import com.roofingcrm.api.v1.estimate.EstimateDto;
import com.roofingcrm.api.v1.estimate.EstimateItemDto;
import com.roofingcrm.api.v1.estimate.EstimateSummaryDto;
import com.roofingcrm.domain.entity.Estimate;
import com.roofingcrm.domain.entity.EstimateItem;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class EstimateMapper {

    public EstimateDto toDto(Estimate entity) {
        EstimateDto dto = new EstimateDto();
        dto.setId(entity.getId());
        dto.setStatus(entity.getStatus());
        dto.setTitle(entity.getTitle());
        dto.setNotes(entity.getNotesForCustomer());
        dto.setIssueDate(entity.getIssueDate());
        dto.setValidUntil(entity.getValidUntil());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getJob() != null) {
            dto.setJobId(entity.getJob().getId());
            if (entity.getJob().getCustomer() != null) {
                dto.setCustomerId(entity.getJob().getCustomer().getId());
            }
        }

        List<EstimateItem> items = entity.getItems();
        if (items != null && Hibernate.isInitialized(items)) {
            List<EstimateItemDto> itemDtos = new ArrayList<>();
            for (EstimateItem item : items) {
                EstimateItemDto itemDto = new EstimateItemDto();
                itemDto.setId(item.getId());
                itemDto.setName(item.getName());
                itemDto.setDescription(item.getDescription());
                itemDto.setQuantity(item.getQuantity());
                itemDto.setUnitPrice(item.getUnitPrice());
                itemDto.setUnit(item.getUnit());
                itemDtos.add(itemDto);
            }
            dto.setItems(itemDtos);
        }

        BigDecimal subtotal = entity.getSubtotal();
        if (subtotal == null && items != null && Hibernate.isInitialized(items)) {
            subtotal = computeSubtotal(items);
        }
        dto.setSubtotal(subtotal);

        BigDecimal total = entity.getTotal();
        if (total == null) {
            total = subtotal;
        }
        dto.setTotal(total);

        return dto;
    }

    public EstimateSummaryDto toSummaryDto(Estimate entity) {
        EstimateSummaryDto dto = new EstimateSummaryDto();
        dto.setId(entity.getId());
        dto.setStatus(entity.getStatus());
        dto.setTitle(entity.getTitle());
        dto.setNotes(entity.getNotesForCustomer());
        dto.setIssueDate(entity.getIssueDate());
        dto.setValidUntil(entity.getValidUntil());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setSubtotal(entity.getSubtotal());
        dto.setTotal(entity.getTotal());

        if (entity.getJob() != null) {
            dto.setJobId(entity.getJob().getId());
            if (entity.getJob().getCustomer() != null) {
                dto.setCustomerId(entity.getJob().getCustomer().getId());
            }
        }
        return dto;
    }

    private BigDecimal computeSubtotal(List<EstimateItem> items) {
        return items.stream()
                .map(EstimateItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
