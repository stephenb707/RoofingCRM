package com.roofingcrm.service.invoice;

import com.roofingcrm.domain.entity.Customer;
import com.roofingcrm.domain.entity.Invoice;
import com.roofingcrm.domain.entity.InvoiceItem;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.enums.InvoiceStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InvoiceMapperTest {

    private final InvoiceMapper mapper = new InvoiceMapper();

    @Test
    void toDto_withNullItems_doesNotMapItems() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setInvoiceNumber("INV-1");
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setTotal(new BigDecimal("1200.00"));
        invoice.setItems(null);

        var dto = mapper.toDto(invoice);

        assertNotNull(dto);
        assertEquals("INV-1", dto.getInvoiceNumber());
        assertNull(dto.getItems());
        assertEquals(new BigDecimal("1200.00"), dto.getTotal());
    }

    @Test
    void toDto_withInitializedItems_mapsItems() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setInvoiceNumber("INV-2");
        invoice.setStatus(InvoiceStatus.SENT);
        invoice.setTotal(new BigDecimal("200.00"));

        InvoiceItem item = new InvoiceItem();
        item.setId(UUID.randomUUID());
        item.setName("Labor");
        item.setQuantity(new BigDecimal("2"));
        item.setUnitPrice(new BigDecimal("100.00"));
        item.setLineTotal(new BigDecimal("200.00"));
        item.setSortOrder(0);

        invoice.setItems(new ArrayList<>(List.of(item)));

        var dto = mapper.toDto(invoice);

        assertNotNull(dto.getItems());
        assertEquals(1, dto.getItems().size());
        assertEquals("Labor", dto.getItems().get(0).getName());
        assertEquals(new BigDecimal("200.00"), dto.getItems().get(0).getLineTotal());
    }

    @Test
    void toDto_mapsCustomerNameAndEmailFromJobCustomer() {
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setInvoiceNumber("INV-3");
        invoice.setStatus(InvoiceStatus.DRAFT);

        Customer customer = new Customer();
        customer.setFirstName("Jane");
        customer.setLastName("Doe");
        customer.setEmail("jane@example.com");

        Job job = new Job();
        job.setId(UUID.randomUUID());
        job.setCustomer(customer);
        invoice.setJob(job);

        var dto = mapper.toDto(invoice);

        assertEquals("Jane Doe", dto.getCustomerName());
        assertEquals("jane@example.com", dto.getCustomerEmail());
    }
}
