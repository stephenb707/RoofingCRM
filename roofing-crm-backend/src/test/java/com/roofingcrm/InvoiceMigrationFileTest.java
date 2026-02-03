package com.roofingcrm;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test: verifies that Flyway migrations include updated_by_user_id
 * for the invoices table (expected by Invoice/TenantAuditedEntity).
 * Runs without Docker; prevents migration/entity schema drift.
 */
class InvoiceMigrationFileTest {

    @Test
    void v14_migration_adds_updated_by_user_id_to_invoices() throws IOException {
        String path = "db/migration/V14__invoice_audit_columns.sql";
        String content;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            assert is != null : "V14 migration file must exist";
            content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertTrue(
                content.contains("updated_by_user_id"),
                "V14 migration must add updated_by_user_id (required by TenantAuditedEntity)");
        assertTrue(
                content.toLowerCase().contains("invoices"),
                "V14 migration must alter invoices table");
    }
}
