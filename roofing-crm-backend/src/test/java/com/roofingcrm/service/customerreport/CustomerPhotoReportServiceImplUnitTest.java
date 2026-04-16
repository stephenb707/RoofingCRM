package com.roofingcrm.service.customerreport;

import com.roofingcrm.api.v1.customerreport.CustomerPhotoReportDto;
import com.roofingcrm.api.v1.customerreport.CustomerPhotoReportSectionRequest;
import com.roofingcrm.api.v1.customerreport.SendCustomerPhotoReportEmailRequest;
import com.roofingcrm.api.v1.customerreport.UpsertCustomerPhotoReportRequest;
import com.roofingcrm.domain.entity.Attachment;
import com.roofingcrm.domain.entity.Customer;
import com.roofingcrm.domain.entity.CustomerPhotoReport;
import com.roofingcrm.domain.entity.CustomerPhotoReportSection;
import com.roofingcrm.domain.entity.CustomerPhotoReportSectionPhoto;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.repository.AttachmentRepository;
import com.roofingcrm.domain.repository.CustomerPhotoReportRepository;
import com.roofingcrm.domain.repository.CustomerPhotoReportSectionPhotoRepository;
import com.roofingcrm.domain.repository.CustomerRepository;
import com.roofingcrm.domain.repository.JobRepository;
import com.roofingcrm.service.mail.EmailService;
import com.roofingcrm.service.report.CustomerPhotoReportPdfGenerator;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CustomerPhotoReportServiceImplUnitTest {

    @Mock
    private TenantAccessService tenantAccessService;
    @Mock
    private CustomerPhotoReportRepository reportRepository;
    @Mock
    private CustomerPhotoReportSectionPhotoRepository sectionPhotoRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private JobRepository jobRepository;
    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private CustomerPhotoReportPdfGenerator pdfGenerator;
    @Mock
    private EmailService emailService;

    private CustomerPhotoReportServiceImpl service;

    private Tenant tenant;
    private Customer customer;
    private Job job;
    private Attachment frontPhoto;
    private Attachment rearPhoto;
    private UUID tenantId;
    private UUID userId;
    private final AtomicReference<CustomerPhotoReport> storedReport = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        service = new CustomerPhotoReportServiceImpl(
                tenantAccessService,
                reportRepository,
                sectionPhotoRepository,
                customerRepository,
                jobRepository,
                attachmentRepository,
                pdfGenerator,
                emailService
        );

        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();

        tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("Acme");

        customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setTenant(tenant);
        customer.setFirstName("Jane");
        customer.setLastName("Doe");
        customer.setEmail("jane@example.com");

        job = new Job();
        job.setId(UUID.randomUUID());
        job.setTenant(tenant);
        job.setCustomer(customer);

        frontPhoto = buildAttachment(UUID.randomUUID(), "front.jpg");
        rearPhoto = buildAttachment(UUID.randomUUID(), "rear.jpg");

        when(tenantAccessService.requireAnyRole(eq(tenantId), eq(userId), any(), anyString())).thenReturn(null);
        when(tenantAccessService.loadTenantForUserOrThrow(tenantId, userId)).thenReturn(tenant);
        when(customerRepository.findByIdAndTenantAndArchivedFalse(customer.getId(), tenant)).thenReturn(Optional.of(customer));
        when(attachmentRepository.findByIdAndTenantAndArchivedFalse(frontPhoto.getId(), tenant)).thenReturn(Optional.of(frontPhoto));
        when(attachmentRepository.findByIdAndTenantAndArchivedFalse(rearPhoto.getId(), tenant)).thenReturn(Optional.of(rearPhoto));

        when(reportRepository.save(any(CustomerPhotoReport.class))).thenAnswer(invocation -> {
            CustomerPhotoReport report = invocation.getArgument(0);
            assignIds(report);
            storedReport.set(report);
            return report;
        });
        when(reportRepository.loadDetailed(any(UUID.class), eq(tenant))).thenAnswer(invocation ->
                Optional.ofNullable(storedReport.get()));
        lenient().when(reportRepository.findById(any(UUID.class))).thenAnswer(invocation ->
                Optional.ofNullable(storedReport.get()));
        when(sectionPhotoRepository.findBySectionIdInWithAttachmentFetched(any())).thenAnswer(invocation ->
                storedReport.get() == null
                        ? List.of()
                        : storedReport.get().getSections().stream().flatMap(section -> section.getPhotos().stream()).toList());
    }

    @Test
    void create_keepsAttachmentSelectionsScopedToEachSection() {
        CustomerPhotoReportDto dto = service.create(tenantId, userId, buildRequest(
                List.of(frontPhoto.getId()),
                List.of(rearPhoto.getId())
        ));

        assertEquals(2, dto.getSections().size());
        assertEquals(List.of(frontPhoto.getId()),
                dto.getSections().get(0).getPhotos().stream().map(p -> p.getAttachmentId()).toList());
        assertEquals(List.of(rearPhoto.getId()),
                dto.getSections().get(1).getPhotos().stream().map(p -> p.getAttachmentId()).toList());
    }

    @Test
    void update_replacesSectionPhotosWithoutLeakingAcrossSections() {
        CustomerPhotoReportDto created = service.create(tenantId, userId, buildRequest(
                List.of(frontPhoto.getId()),
                List.of(rearPhoto.getId())
        ));

        CustomerPhotoReportDto updated = service.update(tenantId, userId, created.getId(), buildRequest(
                List.of(rearPhoto.getId()),
                List.of(frontPhoto.getId(), rearPhoto.getId())
        ));

        assertEquals(List.of(rearPhoto.getId()),
                updated.getSections().get(0).getPhotos().stream().map(p -> p.getAttachmentId()).toList());
        assertEquals(List.of(frontPhoto.getId(), rearPhoto.getId()),
                updated.getSections().get(1).getPhotos().stream().map(p -> p.getAttachmentId()).toList());
    }

    @Test
    void sendEmail_attachesGeneratedPdfAndUsesCustomerFacingFields() {
        job.setScheduledStartDate(java.time.LocalDate.of(2026, 4, 12));
        service.create(tenantId, userId, buildRequest(List.of(frontPhoto.getId()), List.of(rearPhoto.getId())));
        when(pdfGenerator.generate(any(CustomerPhotoReport.class), eq(tenant))).thenReturn("pdf".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        SendCustomerPhotoReportEmailRequest request = new SendCustomerPhotoReportEmailRequest();
        request.setRecipientEmail("customer@example.com");
        request.setRecipientName("Jane Doe");
        request.setSubject("Acme - Inspection");
        request.setMessage("Attached is your report.");

        var response = service.sendEmail(tenantId, userId, storedReport.get().getId(), request);

        assertTrue(response.isSuccess());
        verify(emailService).send(argThat(message ->
                "customer@example.com".equals(message.toEmail())
                        && message.attachments() != null
                        && message.attachments().size() == 1
                        && message.attachments().get(0).filename().endsWith(".pdf")
                        && "application/pdf".equals(message.attachments().get(0).contentType())));
    }

    private UpsertCustomerPhotoReportRequest buildRequest(List<UUID> sectionOne, List<UUID> sectionTwo) {
        UpsertCustomerPhotoReportRequest request = new UpsertCustomerPhotoReportRequest();
        request.setCustomerId(customer.getId());
        request.setTitle("Inspection");

        CustomerPhotoReportSectionRequest first = new CustomerPhotoReportSectionRequest();
        first.setTitle("Front");
        first.setBody("Front notes");
        first.setAttachmentIds(sectionOne);

        CustomerPhotoReportSectionRequest second = new CustomerPhotoReportSectionRequest();
        second.setTitle("Rear");
        second.setBody("Rear notes");
        second.setAttachmentIds(sectionTwo);

        request.setSections(List.of(first, second));
        return request;
    }

    private Attachment buildAttachment(UUID id, String fileName) {
        Attachment attachment = new Attachment();
        attachment.setId(id);
        attachment.setFileName(fileName);
        attachment.setContentType("image/jpeg");
        attachment.setFileSize(100L);
        attachment.setTenant(tenant);
        attachment.setJob(job);
        return attachment;
    }

    private void assignIds(CustomerPhotoReport report) {
        if (report.getId() == null) {
            report.setId(UUID.randomUUID());
        }
        for (CustomerPhotoReportSection section : report.getSections()) {
            if (section.getId() == null) {
                section.setId(UUID.randomUUID());
            }
            for (CustomerPhotoReportSectionPhoto photo : section.getPhotos()) {
                if (photo.getId() == null) {
                    photo.setId(UUID.randomUUID());
                }
            }
        }
    }
}
