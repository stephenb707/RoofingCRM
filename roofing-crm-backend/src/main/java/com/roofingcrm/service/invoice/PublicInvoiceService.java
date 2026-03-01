package com.roofingcrm.service.invoice;

import com.roofingcrm.api.publicapi.invoice.PublicInvoiceDto;
import com.roofingcrm.api.publicapi.invoice.PublicInvoiceItemDto;
import com.roofingcrm.domain.entity.Customer;
import com.roofingcrm.domain.entity.Invoice;
import com.roofingcrm.domain.entity.InvoiceItem;
import com.roofingcrm.domain.repository.InvoiceRepository;
import com.roofingcrm.domain.value.Address;
import com.roofingcrm.service.exception.InvoiceLinkExpiredException;
import com.roofingcrm.service.exception.ResourceNotFoundException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class PublicInvoiceService {

    private final InvoiceRepository invoiceRepository;

    public PublicInvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional(readOnly = true)
    public PublicInvoiceDto getByToken(@NonNull String token) {
        Invoice invoice = invoiceRepository
                .findByPublicTokenAndPublicEnabledTrueAndArchivedFalse(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (invoice.getPublicExpiresAt() != null && Instant.now().isAfter(invoice.getPublicExpiresAt())) {
            throw new InvoiceLinkExpiredException("Link expired");
        }

        return toPublicDto(invoice);
    }

    private PublicInvoiceDto toPublicDto(Invoice invoice) {
        PublicInvoiceDto dto = new PublicInvoiceDto();
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setStatus(invoice.getStatus());
        dto.setIssuedAt(invoice.getIssuedAt());
        dto.setDueAt(invoice.getDueAt());
        dto.setSentAt(invoice.getSentAt());
        dto.setTotal(invoice.getTotal());
        dto.setNotes(invoice.getNotes());
        dto.setPublicExpiresAt(invoice.getPublicExpiresAt());

        if (invoice.getJob() != null && invoice.getJob().getCustomer() != null) {
            Customer c = invoice.getJob().getCustomer();
            dto.setCustomerName(formatCustomerName(c.getFirstName(), c.getLastName()));
            Address addr = invoice.getJob().getPropertyAddress();
            if (addr == null && c.getBillingAddress() != null) {
                addr = c.getBillingAddress();
            }
            dto.setCustomerAddress(addr != null ? formatAddress(addr) : null);
        }

        List<PublicInvoiceItemDto> items = new ArrayList<>();
        if (invoice.getItems() != null) {
            for (InvoiceItem item : invoice.getItems()) {
                PublicInvoiceItemDto itemDto = new PublicInvoiceItemDto();
                itemDto.setName(item.getName());
                itemDto.setDescription(item.getDescription());
                itemDto.setQuantity(item.getQuantity());
                itemDto.setUnitPrice(item.getUnitPrice());
                itemDto.setUnit(null);
                itemDto.setLineTotal(item.getLineTotal());
                items.add(itemDto);
            }
        }
        dto.setItems(items);
        return dto;
    }

    private static String formatCustomerName(String first, String last) {
        if (first == null && last == null) return null;
        return ((first != null ? first.trim() : "") + " " + (last != null ? last.trim() : "")).trim();
    }

    private static String formatAddress(Address a) {
        if (a == null) return null;
        List<String> parts = new ArrayList<>();
        if (a.getLine1() != null && !a.getLine1().isBlank()) parts.add(a.getLine1().trim());
        if (a.getLine2() != null && !a.getLine2().isBlank()) parts.add(a.getLine2().trim());
        StringBuilder cityState = new StringBuilder();
        if (a.getCity() != null && !a.getCity().isBlank()) cityState.append(a.getCity().trim());
        if (a.getState() != null && !a.getState().isBlank()) {
            if (cityState.length() > 0) cityState.append(", ");
            cityState.append(a.getState().trim());
        }
        if (a.getZip() != null && !a.getZip().isBlank()) {
            if (cityState.length() > 0) cityState.append(" ");
            cityState.append(a.getZip().trim());
        }
        if (cityState.length() > 0) parts.add(cityState.toString());
        return String.join(", ", parts);
    }
}
