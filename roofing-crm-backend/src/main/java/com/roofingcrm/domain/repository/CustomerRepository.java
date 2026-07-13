package com.roofingcrm.domain.repository;

import com.roofingcrm.domain.entity.Customer;
import com.roofingcrm.domain.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    long countByTenantAndArchivedFalse(Tenant tenant);

    Page<Customer> findByTenantAndArchivedFalse(Tenant tenant, Pageable pageable);

    Optional<Customer> findByIdAndTenantAndArchivedFalse(UUID id, Tenant tenant);

    /**
     * Native SQL on purpose: the equivalent JPQL (nested replace()/concat() inside a large OR
     * tree) is pathologically expensive for Hibernate 6.6's ANTLR HQL parser and caused an
     * OutOfMemoryError at repository bootstrap on small heaps. PostgreSQL parses the SQL itself.
     * All user input stays in bound parameters. {@code :digitsOnly} and {@code :qNoSpaces} must
     * be non-null (pass "" to disable those match branches) so the JDBC driver never has to bind
     * an untyped null.
     */
    @Query(nativeQuery = true, value = """
        SELECT c.* FROM customers c
        WHERE c.tenant_id = :tenantId
          AND c.archived = false
          AND (
               lower(c.first_name) LIKE lower('%' || :q || '%')
            OR lower(c.last_name) LIKE lower('%' || :q || '%')
            OR lower(c.email) LIKE lower('%' || :q || '%')
            OR lower(c.first_name || ' ' || c.last_name) LIKE lower('%' || :q || '%')
            OR lower(c.last_name || ' ' || c.first_name) LIKE lower('%' || :q || '%')
            OR (
                 :digitsOnly <> ''
                 AND replace(replace(replace(replace(c.primary_phone, '-', ''), ' ', ''), '(', ''), ')', '') LIKE '%' || :digitsOnly || '%'
               )
            OR (:qNoSpaces <> '' AND (
                 lower(c.first_name || c.last_name) LIKE lower('%' || :qNoSpaces || '%')
                 OR lower(c.last_name || c.first_name) LIKE lower('%' || :qNoSpaces || '%')
               ))
          )
        """,
        countQuery = """
        SELECT count(*) FROM customers c
        WHERE c.tenant_id = :tenantId
          AND c.archived = false
          AND (
               lower(c.first_name) LIKE lower('%' || :q || '%')
            OR lower(c.last_name) LIKE lower('%' || :q || '%')
            OR lower(c.email) LIKE lower('%' || :q || '%')
            OR lower(c.first_name || ' ' || c.last_name) LIKE lower('%' || :q || '%')
            OR lower(c.last_name || ' ' || c.first_name) LIKE lower('%' || :q || '%')
            OR (
                 :digitsOnly <> ''
                 AND replace(replace(replace(replace(c.primary_phone, '-', ''), ' ', ''), '(', ''), ')', '') LIKE '%' || :digitsOnly || '%'
               )
            OR (:qNoSpaces <> '' AND (
                 lower(c.first_name || c.last_name) LIKE lower('%' || :qNoSpaces || '%')
                 OR lower(c.last_name || c.first_name) LIKE lower('%' || :qNoSpaces || '%')
               ))
          )
        """)
    Page<Customer> search(@Param("tenantId") UUID tenantId, @Param("q") String q, @Param("digitsOnly") String digitsOnly, @Param("qNoSpaces") String qNoSpaces, Pageable pageable);
}
