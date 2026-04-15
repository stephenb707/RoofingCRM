package com.roofingcrm.service.report;

import com.roofingcrm.domain.entity.Attachment;
import com.roofingcrm.domain.entity.Customer;
import com.roofingcrm.domain.entity.CustomerPhotoReport;
import com.roofingcrm.domain.entity.CustomerPhotoReportSection;
import com.roofingcrm.domain.entity.CustomerPhotoReportSectionPhoto;
import com.roofingcrm.domain.entity.Job;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.value.Address;
import com.roofingcrm.storage.AttachmentStorageService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class CustomerPhotoReportPdfGeneratorTest {

    @Test
    void generate_includesWrappedTextAndImageContentWithoutCrashing() throws Exception {
        TimeZone previous = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/Denver"));
        try {
        AttachmentStorageService storageService = mock(AttachmentStorageService.class);
        CustomerPhotoReportPdfGenerator generator = new CustomerPhotoReportPdfGenerator(storageService);

        CustomerPhotoReport report = buildReportWithImage(storageService, "roof-photo.png", "report/roof-photo.png",
                pngBytes(1200, 800, Color.ORANGE));
        Tenant tenant = new Tenant();
        tenant.setName("Acme Roofing");

        byte[] pdf = generator.generate(report, tenant);

        try (PDDocument document = PDDocument.load(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(document.getNumberOfPages() >= 1);
            assertTrue(text.contains("Acme Roofing"));
            assertTrue(text.contains("Front elevation"));
            assertTrue(text.contains("Roof inspection summary"));
            assertTrue(text.contains("Related job:"));
            assertTrue(text.contains("123 Main St"));
            assertTrue(text.contains("April 14, 2026"));
            assertTrue(!text.contains("Photo 1.1"));
            assertTrue(!text.contains("roof-photo.png"));
        }
        } finally {
            TimeZone.setDefault(previous);
        }
    }

    @Test
    void generate_handlesVeryTallImagesAndLongSectionsAcrossPages() throws Exception {
        TimeZone previous = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/Denver"));
        try {
        AttachmentStorageService storageService = mock(AttachmentStorageService.class);
        CustomerPhotoReportPdfGenerator generator = new CustomerPhotoReportPdfGenerator(storageService);

        CustomerPhotoReport report = buildReportWithImage(storageService, "tall-photo.png", "report/tall-photo.png",
                pngBytes(160, 2400, Color.BLUE));
        report.getSections().getFirst().setBody("""
                This section includes a long narrative that should wrap cleanly without overlapping lines.
                """.repeat(40));
        Tenant tenant = new Tenant();
        tenant.setName("Acme Roofing");

        byte[] pdf = generator.generate(report, tenant);

        try (PDDocument document = PDDocument.load(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(document.getNumberOfPages() >= 2);
            assertTrue(text.contains("Front elevation"));
            assertTrue(!text.contains("Photo 1.1"));
        }
        } finally {
            TimeZone.setDefault(previous);
        }
    }

    private CustomerPhotoReport buildReportWithImage(AttachmentStorageService storageService,
                                                     String fileName,
                                                     String storageKey,
                                                     byte[] imageBytes) {
        when(storageService.loadAsStream(storageKey)).thenReturn(new java.io.ByteArrayInputStream(imageBytes));

        Customer customer = new Customer();
        customer.setFirstName("Jane");
        customer.setLastName("Doe");

        Job job = new Job();
        job.setCustomer(customer);
        job.setPropertyAddress(new Address("123 Main St", null, "Denver", "CO", "80202", "US"));

        Attachment attachment = new Attachment();
        attachment.setFileName(fileName);
        attachment.setContentType("image/png");
        attachment.setStorageKey(storageKey);

        CustomerPhotoReportSectionPhoto photo = new CustomerPhotoReportSectionPhoto();
        photo.setAttachment(attachment);
        photo.setSortOrder(0);

        CustomerPhotoReportSection section = new CustomerPhotoReportSection();
        section.setSortOrder(0);
        section.setTitle("Front elevation");
        section.setBody("Roof inspection summary with detailed notes for the customer.");
        section.getPhotos().add(photo);

        CustomerPhotoReport report = new CustomerPhotoReport();
        report.setCustomer(customer);
        report.setJob(job);
        report.setTitle("Customer photo report");
        report.setSummary("Roof inspection summary");
        report.setCreatedAt(java.time.Instant.parse("2026-04-10T00:00:00Z"));
        report.setUpdatedAt(java.time.Instant.parse("2026-04-15T01:30:00Z"));
        report.getSections().add(section);
        return report;
    }

    private static byte[] pngBytes(int width, int height, Color color) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, color.getRGB());
            }
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        }
    }
}
