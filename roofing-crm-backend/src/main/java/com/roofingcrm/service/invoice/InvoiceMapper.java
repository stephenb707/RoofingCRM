package com.roofingcrm.service.invoice;

import com.roofingcrm.api.v1.invoice.InvoiceDto;
import com.roofingcrm.api.v1.invoice.InvoiceItemDto;
import com.roofingcrm.api.v1.invoice.InvoiceSummaryDto;
import com.roofingcrm.domain.entity.Invoice;
import com.roofingcrm.domain.entity.InvoiceItem;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class InvoiceMapper {

    public InvoiceDto toDto(Invoice invoice) {
        InvoiceDto dto = new InvoiceDto();
        dto.setId(invoice.getId());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setStatus(invoice.getStatus());
        dto.setIssuedAt(invoice.getIssuedAt());
        dto.setSentAt(invoice.getSentAt());
        dto.setDueAt(invoice.getDueAt());
        dto.setPaidAt(invoice.getPaidAt());
        dto.setTotal(invoice.getTotal());
        dto.setNotes(invoice.getNotes());
        dto.setJobId(invoice.getJob() != null ? invoice.getJob().getId() : null);
        dto.setEstimateId(invoice.getEstimate() != null ? invoice.getEstimate().getId() : null);
        dto.setCreatedAt(invoice.getCreatedAt());
        dto.setUpdatedAt(invoice.getUpdatedAt());

        List<InvoiceItem> items = invoice.getItems();
        if (items != null && Hibernate.isInitialized(items)) {
            List<InvoiceItemDto> itemDtos = new ArrayList<>();
            for (InvoiceItem item : items) {
                InvoiceItemDto itemDto = new InvoiceItemDto();
                itemDto.setId(item.getId());
                itemDto.setName(item.getName());
                itemDto.setDescription(item.getDescription());
                itemDto.setQuantity(item.getQuantity());
                itemDto.setUnitPrice(item.getUnitPrice());
                itemDto.setLineTotal(item.getLineTotal());
                itemDto.setSortOrder(item.getSortOrder());
                itemDtos.add(itemDto);
            }
            dto.setItems(itemDtos);
        }
        return dto;
    }

    public InvoiceSummaryDto toSummaryDto(Invoice invoice) {
        InvoiceSummaryDto dto = new InvoiceSummaryDto();
        dto.setId(invoice.getId());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setStatus(invoice.getStatus());
        dto.setIssuedAt(invoice.getIssuedAt());
        dto.setSentAt(invoice.getSentAt());
        dto.setDueAt(invoice.getDueAt());
        dto.setPaidAt(invoice.getPaidAt());
        dto.setTotal(invoice.getTotal());
        dto.setNotes(invoice.getNotes());
        dto.setJobId(invoice.getJob() != null ? invoice.getJob().getId() : null);
        dto.setEstimateId(invoice.getEstimate() != null ? invoice.getEstimate().getId() : null);
        dto.setCreatedAt(invoice.getCreatedAt());
        dto.setUpdatedAt(invoice.getUpdatedAt());
        return dto;
    }
}
