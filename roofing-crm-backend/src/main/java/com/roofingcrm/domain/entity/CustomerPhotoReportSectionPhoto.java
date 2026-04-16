package com.roofingcrm.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "customer_photo_report_section_photos")
@Getter
@Setter
@NoArgsConstructor
public class CustomerPhotoReportSectionPhoto extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private CustomerPhotoReportSection section;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attachment_id", nullable = false)
    private Attachment attachment;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
