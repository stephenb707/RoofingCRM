package com.roofingcrm.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_photo_reports")
@Getter
@Setter
@NoArgsConstructor
public class CustomerPhotoReport extends TenantAuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "report_type", length = 120)
    private String reportType;

    @Column(columnDefinition = "text")
    private String summary;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<CustomerPhotoReportSection> sections = new ArrayList<>();
}
