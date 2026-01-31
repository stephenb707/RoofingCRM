package com.roofingcrm;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TestDatabaseCleaner {

    private final JdbcTemplate jdbcTemplate;

    public TestDatabaseCleaner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Resets the database to an empty state for tests.
     * - Truncates all tables in schema "public" (except flyway_schema_history)
     * - Restarts identity sequences
     * - CASCADE handles FK dependencies (e.g., activity_events -> users)
     */
    @Transactional
    public void reset() {
        jdbcTemplate.execute("""
            DO $$
            DECLARE
                stmt text;
            BEGIN
                SELECT 'TRUNCATE TABLE ' ||
                       string_agg(format('%I.%I', schemaname, tablename), ', ') ||
                       ' RESTART IDENTITY CASCADE'
                INTO stmt
                FROM pg_tables
                WHERE schemaname = 'public'
                  AND tablename <> 'flyway_schema_history';

                IF stmt IS NOT NULL THEN
                    EXECUTE stmt;
                END IF;
            END $$;
        """);
    }
}
