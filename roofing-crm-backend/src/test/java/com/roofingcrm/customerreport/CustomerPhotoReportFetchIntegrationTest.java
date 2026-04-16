package com.roofingcrm.customerreport;

import com.roofingcrm.AbstractIntegrationTest;
import com.roofingcrm.domain.entity.Attachment;
import com.roofingcrm.domain.entity.Customer;
import com.roofingcrm.domain.entity.CustomerPhotoReport;
import com.roofingcrm.domain.entity.CustomerPhotoReportSection;
import com.roofingcrm.domain.entity.CustomerPhotoReportSectionPhoto;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.repository.AttachmentRepository;
import com.roofingcrm.domain.repository.CustomerPhotoReportRepository;
import com.roofingcrm.domain.repository.CustomerPhotoReportSectionPhotoRepository;
import com.roofingcrm.domain.repository.CustomerRepository;
import com.roofingcrm.domain.repository.TenantRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression: loading report detail must not use one EntityGraph that fetch-joins both
 * CustomerPhotoReport.sections and CustomerPhotoReportSection.photos (two bags).
 */
@SuppressWarnings("null")
class CustomerPhotoReportFetchIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private CustomerPhotoReportRepository reportRepository;

    @Autowired
    private CustomerPhotoReportSectionPhotoRepository sectionPhotoRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @Transactional
    void loadDetailed_thenBatchFetchPhotos_initializesPhotosWithoutMultipleBagFetch() {
        Tenant tenant = new Tenant();
        tenant.setName("Photo Report Tenant");
        tenantRepository.save(tenant);

        Customer customer = new Customer();
        customer.setTenant(tenant);
        customer.setFirstName("Jane");
        customer.setLastName("Doe");
        customerRepository.save(customer);

        Attachment attachment = new Attachment();
        attachment.setTenant(tenant);
        attachment.setFileName("roof.jpg");
        attachment.setContentType("image/jpeg");
        attachment.setFileSize(10L);
        attachment.setStorageProvider("LOCAL");
        attachment.setStorageKey("test/roof.jpg");
        attachmentRepository.save(attachment);

        CustomerPhotoReport report = new CustomerPhotoReport();
        report.setTenant(tenant);
        report.setCustomer(customer);
        report.setTitle("Inspection");
        CustomerPhotoReportSection section = new CustomerPhotoReportSection();
        section.setReport(report);
        section.setSortOrder(0);
        section.setTitle("Front elevation");
        section.setBody("Notes");
        CustomerPhotoReportSectionPhoto photo = new CustomerPhotoReportSectionPhoto();
        photo.setSection(section);
        photo.setAttachment(attachment);
        photo.setSortOrder(0);
        section.getPhotos().add(photo);
        report.getSections().add(section);
        reportRepository.save(report);

        UUID reportId = report.getId();
        UUID tenantId = tenant.getId();

        entityManager.flush();
        entityManager.clear();

        Tenant tenantRef = tenantRepository.getReferenceById(tenantId);
        Optional<CustomerPhotoReport> loaded = reportRepository.loadDetailed(reportId, tenantRef);
        assertTrue(loaded.isPresent());
        CustomerPhotoReport r = loaded.get();

        List<UUID> sectionIds = r.getSections().stream().map(CustomerPhotoReportSection::getId).toList();
        assertEquals(1, sectionIds.size());
        sectionPhotoRepository.findBySectionIdInWithAttachmentFetched(sectionIds);
        for (CustomerPhotoReportSection s : r.getSections()) {
            Hibernate.initialize(s.getPhotos());
        }

        assertEquals(1, r.getSections().getFirst().getPhotos().size());
        assertNotNull(r.getSections().getFirst().getPhotos().getFirst().getAttachment().getFileName());
    }
}
