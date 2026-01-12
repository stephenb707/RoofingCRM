package com.roofingcrm.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a communication event (email, SMS, call, note) associated
 * with various parent entities (leads, jobs, customers, etc.).
 * Tracks both inbound and outbound communications.
 */
@Entity
@Table(
    name = "communication_logs",
    indexes = {
        @Index(name = "idx_comm_tenant_parent", columnList = "tenant_id, parentType, parentId"),
        @Index(name = "idx_comm_tenant_channel", columnList = "tenant_id, channel")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class CommunicationLog extends TenantAuditedEntity {

    @Column(nullable = false, length = 50)
    private String parentType; // "LEAD", "JOB", "CUSTOMER"

    @Column(nullable = false)
    private UUID parentId;

    @Column(nullable = false, length = 20)
    private String channel; // "EMAIL", "SMS", "CALL", "NOTE"

    @Column(nullable = false)
    private Instant occurredAt;

    private String direction; // "OUTBOUND", "INBOUND"

    // optional: phone number or email address
    private String target;

    @Column(columnDefinition = "text")
    private String content;

    // For external providers (Twilio message SID, email provider id, etc.)
    private String externalId;
}
