package com.roofingcrm.api.publicapi.invoice;

import com.roofingcrm.service.invoice.PublicInvoiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/invoices")
public class PublicInvoiceController {

    private final PublicInvoiceService publicInvoiceService;

    public PublicInvoiceController(PublicInvoiceService publicInvoiceService) {
        this.publicInvoiceService = publicInvoiceService;
    }

    @GetMapping("/{token}")
    public ResponseEntity<PublicInvoiceDto> getByToken(@PathVariable("token") @NonNull String token) {
        PublicInvoiceDto dto = publicInvoiceService.getByToken(token);
        return ResponseEntity.ok(dto);
    }
}
