package com.roofingcrm;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regression test: verifies that the invoices table has all columns expected by
 * the Invoice JPA entity (which extends TenantAuditedEntity).
 * Prevents schema validation errors when entity mappings and migrations drift.
 */
class InvoiceSchemaIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void invoices_table_has_updated_by_user_id_column() {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'invoices'
                  AND column_name = 'updated_by_user_id'
                """,
                Integer.class);
        assertEquals(1, count, "invoices table must have updated_by_user_id column (TenantAuditedEntity)");
    }

    @Test
    void invoices_table_has_created_by_user_id_column() {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'invoices'
                  AND column_name = 'created_by_user_id'
                """,
                Integer.class);
        assertEquals(1, count, "invoices table must have created_by_user_id column (TenantAuditedEntity)");
    }
}
