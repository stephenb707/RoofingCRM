package com.roofingcrm.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Represents a communication event (email, SMS, call, note) associated
 * with leads or jobs. Tracks both inbound and outbound communications.
 */
@Entity
@Table(
    name = "communication_logs",
    indexes = {
        @Index(name = "idx_comm_tenant_lead_occurred", columnList = "tenant_id, lead_id, occurred_at"),
        @Index(name = "idx_comm_tenant_job_occurred", columnList = "tenant_id, job_id, occurred_at"),
        @Index(name = "idx_comm_tenant_channel", columnList = "tenant_id, channel")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class CommunicationLog extends TenantAuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @Column(nullable = false, length = 20)
    private String channel; // "EMAIL", "SMS", "CALL", "NOTE"

    private String direction; // "OUTBOUND", "INBOUND", null for NOTE

    private String subject;   // short summary / title

    @Column(columnDefinition = "text")
    private String body;      // free text content

    @Column(nullable = false)
    private Instant occurredAt;

    // For external providers (Twilio message SID, email provider id, etc.)
    private String externalId;
    private String externalProvider;
}
