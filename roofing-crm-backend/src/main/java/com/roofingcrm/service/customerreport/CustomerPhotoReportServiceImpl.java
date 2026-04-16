package com.roofingcrm.service.customerreport;

import com.roofingcrm.api.v1.attachment.AttachmentDto;
import com.roofingcrm.api.v1.customerreport.CustomerPhotoReportDto;
import com.roofingcrm.api.v1.customerreport.CustomerPhotoReportSectionDto;
import com.roofingcrm.api.v1.customerreport.CustomerPhotoReportSectionPhotoDto;
import com.roofingcrm.api.v1.customerreport.CustomerPhotoReportSectionRequest;
import com.roofingcrm.api.v1.customerreport.CustomerPhotoReportSummaryDto;
import com.roofingcrm.api.v1.customerreport.SendCustomerPhotoReportEmailRequest;
import com.roofingcrm.api.v1.customerreport.SendCustomerPhotoReportEmailResponse;
import com.roofingcrm.api.v1.customerreport.UpsertCustomerPhotoReportRequest;
import com.roofingcrm.domain.entity.Attachment;
import com.roofingcrm.domain.entity.Customer;
import com.roofingcrm.domain.entity.CustomerPhotoReport;
import com.roofingcrm.domain.entity.CustomerPhotoReportSection;
import com.roofingcrm.domain.entity.CustomerPhotoReportSectionPhoto;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.UserRole;
import com.roofingcrm.domain.repository.AttachmentRepository;
import com.roofingcrm.domain.repository.CustomerPhotoReportRepository;
import com.roofingcrm.domain.repository.CustomerPhotoReportSectionPhotoRepository;
import com.roofingcrm.domain.repository.CustomerRepository;
import com.roofingcrm.domain.repository.JobRepository;
import com.roofingcrm.service.audit.AuditSupport;
import com.roofingcrm.service.exception.ResourceNotFoundException;
import com.roofingcrm.service.mail.CustomerPhotoReportEmailTemplateBuilder;
import com.roofingcrm.service.mail.EmailAttachment;
import com.roofingcrm.service.mail.EmailService;
import com.roofingcrm.service.report.CustomerPhotoReportPdfGenerator;
import com.roofingcrm.service.tenant.TenantAccessService;
import org.hibernate.Hibernate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class CustomerPhotoReportServiceImpl implements CustomerPhotoReportService {

    @SuppressWarnings("null")
    private static final @NonNull Set<UserRole> ROLES = EnumSet.of(UserRole.OWNER, UserRole.ADMIN, UserRole.SALES);

    private final TenantAccessService tenantAccessService;
    private final CustomerPhotoReportRepository reportRepository;
    private final CustomerPhotoReportSectionPhotoRepository sectionPhotoRepository;
    private final CustomerRepository customerRepository;
    private final JobRepository jobRepository;
    private final AttachmentRepository attachmentRepository;
    private final CustomerPhotoReportPdfGenerator pdfGenerator;
    private final EmailService emailService;
    private final CustomerPhotoReportEmailTemplateBuilder emailTemplateBuilder;

    public CustomerPhotoReportServiceImpl(
            TenantAccessService tenantAccessService,
            CustomerPhotoReportRepository reportRepository,
            CustomerPhotoReportSectionPhotoRepository sectionPhotoRepository,
            CustomerRepository customerRepository,
            JobRepository jobRepository,
            AttachmentRepository attachmentRepository,
            CustomerPhotoReportPdfGenerator pdfGenerator,
            EmailService emailService) {
        this.tenantAccessService = tenantAccessService;
        this.reportRepository = reportRepository;
        this.sectionPhotoRepository = sectionPhotoRepository;
        this.customerRepository = customerRepository;
        this.jobRepository = jobRepository;
        this.attachmentRepository = attachmentRepository;
        this.pdfGenerator = pdfGenerator;
        this.emailService = emailService;
        this.emailTemplateBuilder = new CustomerPhotoReportEmailTemplateBuilder();
    }

    private void requireRole(@NonNull UUID tenantId, @NonNull UUID userId) {
        tenantAccessService.requireAnyRole(tenantId, userId, ROLES,
                "You do not have permission to manage customer photo reports.");
    }

    private CustomerPhotoReport loadReportDetailed(UUID reportId, Tenant tenant,
                                                 Supplier<? extends RuntimeException> notFound) {
        CustomerPhotoReport report = reportRepository.loadDetailed(reportId, tenant).orElseThrow(notFound);
        hydrateSectionPhotos(report);
        return report;
    }

    /**
     * Loads section photos (and attachments) in a second query so we never fetch two JPA "bag"
     * collections (report.sections and section.photos) in the same Hibernate load plan.
     */
    private void hydrateSectionPhotos(CustomerPhotoReport report) {
        if (report.getSections() == null || report.getSections().isEmpty()) {
            return;
        }
        List<UUID> sectionIds = report.getSections().stream()
                .map(CustomerPhotoReportSection::getId)
                .filter(Objects::nonNull)
                .toList();
        if (sectionIds.isEmpty()) {
            return;
        }
        sectionPhotoRepository.findBySectionIdInWithAttachmentFetched(sectionIds);
        for (CustomerPhotoReportSection section : report.getSections()) {
            Hibernate.initialize(section.getPhotos());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerPhotoReportSummaryDto> list(@NonNull UUID tenantId, @NonNull UUID userId) {
        requireRole(tenantId, userId);
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        return reportRepository.findByTenantAndArchivedFalseOrderByUpdatedAtDesc(tenant).stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerPhotoReportDto get(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull UUID reportId) {
        requireRole(tenantId, userId);
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        CustomerPhotoReport report = loadReportDetailed(reportId, tenant,
                () -> new ResourceNotFoundException("Report not found"));
        return toDto(report);
    }

    @Override
    @Transactional
    public CustomerPhotoReportDto create(@NonNull UUID tenantId, @NonNull UUID userId,
 UpsertCustomerPhotoReportRequest request) {
        requireRole(tenantId, userId);
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        Customer customer = customerRepository.findByIdAndTenantAndArchivedFalse(request.getCustomerId(), tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        Job job = resolveJob(tenant, customer, request.getJobId());

        CustomerPhotoReport report = new CustomerPhotoReport();
        report.setTenant(tenant);
        report.setCreatedByUserId(userId);
        report.setUpdatedByUserId(userId);
        report.setCustomer(customer);
        report.setJob(job);
        applyMetadata(report, request);
        replaceSectionsFromRequest(report, tenant, customer, job, request);

        CustomerPhotoReport saved = reportRepository.save(report);
        reportRepository.flush();
        CustomerPhotoReport reloaded = loadReportDetailed(Objects.requireNonNull(saved.getId()), tenant,
                () -> new IllegalStateException("Report not found after save"));
        return toDto(reloaded);
    }

    @Override
    @Transactional
    public CustomerPhotoReportDto update(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull UUID reportId,
                                         UpsertCustomerPhotoReportRequest request) {
        requireRole(tenantId, userId);
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        CustomerPhotoReport report = reportRepository.findById(reportId)
                .filter(r -> r.getTenant().getId().equals(tenantId) && !r.isArchived())
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));

        Customer customer = customerRepository.findByIdAndTenantAndArchivedFalse(request.getCustomerId(), tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        Job job = resolveJob(tenant, customer, request.getJobId());

        report.setCustomer(customer);
        report.setJob(job);
        applyMetadata(report, request);
        report.getSections().clear();
        replaceSectionsFromRequest(report, tenant, customer, job, request);
        AuditSupport.touchForUpdate(report, userId);

        reportRepository.save(report);
        reportRepository.flush();
        CustomerPhotoReport reloaded = loadReportDetailed(reportId, tenant,
                () -> new IllegalStateException("Report not found after update"));
        return toDto(reloaded);
    }

    @Override
    @Transactional
    public void archive(@NonNull UUID tenantId, @NonNull UUID userId, @NonNull UUID reportId) {
        requireRole(tenantId, userId);
        tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        CustomerPhotoReport report = reportRepository.findById(reportId)
                .filter(r -> r.getTenant().getId().equals(tenantId) && !r.isArchived())
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        report.setArchived(true);
        report.setArchivedAt(Instant.now());
        AuditSupport.touchForUpdate(report, userId);
        reportRepository.save(report);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerPhotoReportPdfExport exportPdf(@NonNull UUID tenantId, @NonNull UUID userId,
 @NonNull UUID reportId) {
        requireRole(tenantId, userId);
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        CustomerPhotoReport report = loadReportDetailed(reportId, tenant,
                () -> new ResourceNotFoundException("Report not found"));
        return buildPdfExport(report, tenant);
    }

    @Override
    @Transactional(readOnly = true)
    public SendCustomerPhotoReportEmailResponse sendEmail(@NonNull UUID tenantId, @NonNull UUID userId,
                                                          @NonNull UUID reportId,
                                                          @NonNull SendCustomerPhotoReportEmailRequest request) {
        requireRole(tenantId, userId);
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        CustomerPhotoReport report = loadReportDetailed(reportId, tenant,
                () -> new ResourceNotFoundException("Report not found"));
        CustomerPhotoReportPdfExport pdf = buildPdfExport(report, tenant);
        EmailAttachment attachment = new EmailAttachment(
                pdf.filename(),
                Base64.getEncoder().encodeToString(pdf.content()),
                "application/pdf");
        emailService.send(emailTemplateBuilder.build(
                tenant,
                report,
                request.getRecipientEmail(),
                request.getRecipientName(),
                request.getSubject(),
                request.getMessage(),
                attachment
        ));
        SendCustomerPhotoReportEmailResponse response = new SendCustomerPhotoReportEmailResponse();
        response.setSuccess(true);
        response.setSentAt(Instant.now());
        return response;
    }

    private CustomerPhotoReportPdfExport buildPdfExport(CustomerPhotoReport report, Tenant tenant) {
        byte[] content = pdfGenerator.generate(report, tenant);
        String namePart = formatCustomerName(report.getCustomer())
                .replaceAll("[^a-zA-Z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "")
                .toLowerCase(Locale.ROOT);
        if (namePart.isBlank()) {
            namePart = report.getCustomer().getId().toString().substring(0, 8);
        }
        LocalDate reportDate = CustomerPhotoReportPresentationHelper.resolveReportLocalDate(report);
        String filename = "customer-report-" + namePart + "-" + reportDate + ".pdf";
        return new CustomerPhotoReportPdfExport(content, filename);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentDto> listAttachmentCandidates(@NonNull UUID tenantId, @NonNull UUID userId,
                                                      @NonNull UUID customerId, UUID jobId) {
        requireRole(tenantId, userId);
        Tenant tenant = tenantAccessService.loadTenantForUserOrThrow(tenantId, userId);
        Customer customer = customerRepository.findByIdAndTenantAndArchivedFalse(customerId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        List<Attachment> list;
        if (jobId != null) {
            Job job = jobRepository.findByIdAndTenantAndArchivedFalse(jobId, tenant)
                    .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
            if (!job.getCustomer().getId().equals(customer.getId())) {
                throw new IllegalArgumentException("Job does not belong to the selected customer.");
            }
            list = attachmentRepository.findReportableForJob(tenant, jobId);
        } else {
            list = attachmentRepository.findReportableForCustomer(tenant, customer.getId());
        }
        return list.stream().map(this::toAttachmentDto).toList();
    }

    private Job resolveJob(Tenant tenant, Customer customer, UUID jobId) {
        if (jobId == null) {
            return null;
        }
        Job job = jobRepository.findByIdAndTenantAndArchivedFalse(jobId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        if (!job.getCustomer().getId().equals(customer.getId())) {
            throw new IllegalArgumentException("Job does not belong to the selected customer.");
        }
        return job;
    }

    private void applyMetadata(CustomerPhotoReport report, UpsertCustomerPhotoReportRequest request) {
        report.setTitle(request.getTitle().trim());
        report.setReportType(request.getReportType() != null && !request.getReportType().isBlank()
                ? request.getReportType().trim()
                : null);
        report.setSummary(request.getSummary() != null && !request.getSummary().isBlank()
                ? request.getSummary().trim()
                : null);
    }

    private void replaceSectionsFromRequest(CustomerPhotoReport report, Tenant tenant, Customer customer, Job job,
                                            UpsertCustomerPhotoReportRequest request) {
        List<CustomerPhotoReportSectionRequest> sectionRequests = request.getSections() != null
                ? request.getSections()
                : List.of();
        int sort = 0;
        for (CustomerPhotoReportSectionRequest sr : sectionRequests) {
            CustomerPhotoReportSection section = new CustomerPhotoReportSection();
            section.setSortOrder(sort++);
            section.setTitle(sr.getTitle() != null && !sr.getTitle().isBlank()
                    ? sr.getTitle().trim()
                    : "Section");
            section.setBody(sr.getBody() != null ? sr.getBody().trim() : null);
            section.setReport(report);

            List<UUID> ids = sr.getAttachmentIds() != null
                    ? new ArrayList<>(new LinkedHashSet<>(sr.getAttachmentIds()))
                    : List.of();
            int p = 0;
            for (UUID attachmentId : ids) {
                if (attachmentId == null) {
                    continue;
                }
                Attachment att = resolveReportableAttachment(tenant, customer, job, attachmentId);
                CustomerPhotoReportSectionPhoto photo = new CustomerPhotoReportSectionPhoto();
                photo.setSection(section);
                photo.setAttachment(att);
                photo.setSortOrder(p++);
                section.getPhotos().add(photo);
            }
            report.getSections().add(section);
        }
    }

    private Attachment resolveReportableAttachment(Tenant tenant, Customer customer, Job job, UUID attachmentId) {
        Attachment a = attachmentRepository.findByIdAndTenantAndArchivedFalse(attachmentId, tenant)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));
        if (job != null) {
            boolean ok = (a.getJob() != null && a.getJob().getId().equals(job.getId()))
                    || (a.getLead() != null && job.getLead() != null && job.getLead().getId().equals(a.getLead().getId()));
            if (!ok) {
                throw new IllegalArgumentException("Attachment is not linked to the selected job or its lead.");
            }
        } else {
            boolean ok = (a.getJob() != null && a.getJob().getCustomer().getId().equals(customer.getId()))
                    || (a.getLead() != null && a.getLead().getCustomer().getId().equals(customer.getId()));
            if (!ok) {
                throw new IllegalArgumentException("Attachment is not linked to the selected customer.");
            }
        }
        return a;
    }

    private CustomerPhotoReportSummaryDto toSummary(CustomerPhotoReport r) {
        CustomerPhotoReportSummaryDto d = new CustomerPhotoReportSummaryDto();
        d.setId(r.getId());
        d.setTitle(r.getTitle());
        d.setReportType(r.getReportType());
        d.setCustomerId(r.getCustomer().getId());
        d.setCustomerName(formatCustomerName(r.getCustomer()));
        if (r.getJob() != null) {
            d.setJobId(r.getJob().getId());
            d.setJobDisplayName(CustomerPhotoReportPresentationHelper.formatJobDisplay(r.getJob()));
        }
        d.setUpdatedAt(CustomerPhotoReportPresentationHelper.resolveReportDate(r));
        return d;
    }

    private CustomerPhotoReportDto toDto(CustomerPhotoReport r) {
        CustomerPhotoReportDto d = new CustomerPhotoReportDto();
        d.setId(r.getId());
        d.setCustomerId(r.getCustomer().getId());
        d.setCustomerName(formatCustomerName(r.getCustomer()));
        d.setCustomerEmail(r.getCustomer().getEmail());
        if (r.getJob() != null) {
            d.setJobId(r.getJob().getId());
            d.setJobDisplayName(CustomerPhotoReportPresentationHelper.formatJobDisplay(r.getJob()));
        }
        d.setTitle(r.getTitle());
        d.setReportType(r.getReportType());
        d.setSummary(r.getSummary());
        d.setCreatedAt(r.getCreatedAt());
        d.setUpdatedAt(CustomerPhotoReportPresentationHelper.resolveReportDate(r));

        List<CustomerPhotoReportSection> secs = r.getSections() != null
                ? new ArrayList<>(r.getSections())
                : new ArrayList<>();
        secs.sort(Comparator.comparingInt(CustomerPhotoReportSection::getSortOrder));
        for (CustomerPhotoReportSection sec : secs) {
            CustomerPhotoReportSectionDto sd = new CustomerPhotoReportSectionDto();
            sd.setId(sec.getId());
            sd.setSortOrder(sec.getSortOrder());
            sd.setTitle(sec.getTitle());
            sd.setBody(sec.getBody());
            List<CustomerPhotoReportSectionPhoto> photos = sec.getPhotos() != null
                    ? new ArrayList<>(sec.getPhotos())
                    : new ArrayList<>();
            photos.sort(Comparator.comparingInt(CustomerPhotoReportSectionPhoto::getSortOrder));
            for (CustomerPhotoReportSectionPhoto ph : photos) {
                CustomerPhotoReportSectionPhotoDto pd = new CustomerPhotoReportSectionPhotoDto();
                pd.setAttachmentId(ph.getAttachment().getId());
                pd.setSortOrder(ph.getSortOrder());
                sd.getPhotos().add(pd);
            }
            d.getSections().add(sd);
        }
        return d;
    }

    private static String formatCustomerName(Customer c) {
        return CustomerPhotoReportPresentationHelper.formatCustomerName(c);
    }

    private AttachmentDto toAttachmentDto(Attachment entity) {
        AttachmentDto dto = new AttachmentDto();
        dto.setId(entity.getId());
        dto.setFileName(entity.getFileName());
        dto.setContentType(entity.getContentType());
        dto.setFileSize(entity.getFileSize());
        dto.setStorageProvider(entity.getStorageProvider());
        dto.setStorageKey(entity.getStorageKey());
        dto.setDescription(entity.getDescription());
        dto.setTag(entity.getTag());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        if (entity.getLead() != null) {
            dto.setLeadId(entity.getLead().getId());
        }
        if (entity.getJob() != null) {
            dto.setJobId(entity.getJob().getId());
        }
        return dto;
    }
}
