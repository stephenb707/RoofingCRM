package com.roofingcrm.service.invoice;

import com.roofingcrm.domain.entity.Invoice;
import com.roofingcrm.domain.repository.InvoiceRepository;
import com.roofingcrm.security.PublicShareTokenHasher;
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
class PublicInvoiceServiceUnitTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    private PublicInvoiceService service;

    @BeforeEach
    void setUp() {
        service = new PublicInvoiceService(invoiceRepository);
    }

    @Test
    void getByToken_looksUpBySha256Hash() {
        String raw = "customer-facing-token";
        String hash = PublicShareTokenHasher.sha256HexUtf8(raw);
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        when(invoiceRepository.findByPublicTokenHashAndPublicEnabledTrueAndArchivedFalse(eq(hash)))
                .thenReturn(Optional.of(invoice));

        service.getByToken(raw);

        verify(invoiceRepository).findByPublicTokenHashAndPublicEnabledTrueAndArchivedFalse(eq(hash));
    }
}
