package com.roofingcrm.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_photo_report_sections")
@Getter
@Setter
@NoArgsConstructor
public class CustomerPhotoReportSection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private CustomerPhotoReport report;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "text")
    private String body;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<CustomerPhotoReportSectionPhoto> photos = new ArrayList<>();
}
