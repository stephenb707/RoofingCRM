package com.roofingcrm.api.v1.customer;

import com.roofingcrm.security.SecurityUtils;
import com.roofingcrm.service.customer.CustomerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@Validated
public class CustomerController {

    private final CustomerService customerService;

    @Autowired
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<CustomerDto> createCustomer(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @Valid @RequestBody CreateCustomerRequest request) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        CustomerDto created = customerService.createCustomer(tenantId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerDto> updateCustomer(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("id") UUID customerId,
            @Valid @RequestBody UpdateCustomerRequest request) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        CustomerDto updated = customerService.updateCustomer(tenantId, userId, customerId, request);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerDto> getCustomer(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @PathVariable("id") UUID customerId) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        CustomerDto dto = customerService.getCustomer(tenantId, userId, customerId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    public ResponseEntity<Page<CustomerDto>> listCustomers(
            @RequestHeader("X-Tenant-Id") @NonNull UUID tenantId,
            @RequestParam(value = "q", required = false) String q,
            @PageableDefault(size = 20) Pageable pageable) {

        UUID userId = SecurityUtils.getCurrentUserIdOrThrow();
        Page<CustomerDto> page = customerService.listCustomers(tenantId, userId, q, pageable);
        return ResponseEntity.ok(page);
    }
}
