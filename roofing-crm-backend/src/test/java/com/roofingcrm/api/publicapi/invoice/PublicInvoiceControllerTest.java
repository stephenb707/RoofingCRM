package com.roofingcrm.api.publicapi.invoice;

import com.roofingcrm.domain.enums.InvoiceStatus;
import com.roofingcrm.service.exception.InvoiceLinkExpiredException;
import com.roofingcrm.service.invoice.PublicInvoiceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PublicInvoiceController.class)
@AutoConfigureMockMvc(addFilters = false)
@SuppressWarnings("null")
class PublicInvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PublicInvoiceService publicInvoiceService;

    @Test
    void getByToken_returnsInvoiceWhenValid() throws Exception {
        String token = "inv-token-abc";
        PublicInvoiceDto dto = new PublicInvoiceDto();
        dto.setInvoiceNumber("INV-2001");
        dto.setStatus(InvoiceStatus.SENT);
        dto.setTotal(new BigDecimal("1200.00"));
        dto.setCustomerName("Jane Doe");
        dto.setCompanyName("Acme Roofing Co");
        dto.setItems(List.of());

        when(publicInvoiceService.getByToken(eq(token))).thenReturn(dto);

        mockMvc.perform(get("/api/public/invoices/{token}", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber", is("INV-2001")))
                .andExpect(jsonPath("$.status", is("SENT")))
                .andExpect(jsonPath("$.customerName", is("Jane Doe")))
                .andExpect(jsonPath("$.companyName", is("Acme Roofing Co")));
    }

    @Test
    void getByToken_returns410WhenExpired() throws Exception {
        String token = "expired";
        when(publicInvoiceService.getByToken(eq(token)))
                .thenThrow(new InvoiceLinkExpiredException("Link expired"));

        mockMvc.perform(get("/api/public/invoices/{token}", token))
                .andExpect(status().isGone());

        verify(publicInvoiceService).getByToken(eq(token));
    }
}
