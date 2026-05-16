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
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void generate_embedsGifRasterWhenImageIoHasGifWriter() throws Exception {
        TimeZone previous = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/Denver"));
        try {
            byte[] gif = tryWriteRasterAsFormat("gif", 72, 72, Color.RED);
            Assumptions.assumeTrue(gif != null && gif.length > 0);
            AttachmentStorageService storageService = mock(AttachmentStorageService.class);
            CustomerPhotoReportPdfGenerator generator = new CustomerPhotoReportPdfGenerator(storageService);
            CustomerPhotoReport report =
                    buildReportWithImage(storageService, "diagram.gif", "report/diagram.gif", gif);
            Objects.requireNonNull(report.getSections().getFirst().getPhotos().getFirst().getAttachment())
                    .setContentType("image/gif");

            byte[] pdf = generator.generate(report, tenant());

            assertSinglePageHealthyPdf(pdf);
        } finally {
            TimeZone.setDefault(previous);
        }
    }

    @Test
    void generate_embedsJpegRasterWithoutCrashing() throws Exception {
        TimeZone previous = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/Denver"));
        try {
            byte[] jpg = tryWriteRasterAsFormat("jpg", 80, 60, Color.GREEN);
            Assumptions.assumeTrue(jpg != null && jpg.length > 0);
            AttachmentStorageService storageService = mock(AttachmentStorageService.class);
            CustomerPhotoReportPdfGenerator generator = new CustomerPhotoReportPdfGenerator(storageService);
            CustomerPhotoReport report =
                    buildReportWithImage(storageService, "photo.jpg", "report/photo.jpg", jpg);
            Objects.requireNonNull(report.getSections().getFirst().getPhotos().getFirst().getAttachment())
                    .setContentType("image/jpeg");

            byte[] pdf = generator.generate(report, tenant());

            assertSinglePageHealthyPdf(pdf);
        } finally {
            TimeZone.setDefault(previous);
        }
    }

    @Test
    void generate_embedsBmpRasterWithoutCrashing() throws Exception {
        TimeZone previous = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/Denver"));
        try {
            byte[] bmp = tryWriteRasterAsFormat("bmp", 96, 64, Color.MAGENTA);
            AttachmentStorageService storageService = mock(AttachmentStorageService.class);
            CustomerPhotoReportPdfGenerator generator = new CustomerPhotoReportPdfGenerator(storageService);
            CustomerPhotoReport report =
                    buildReportWithImage(storageService, "diagram.bmp", "report/diagram.bmp",
                            Objects.requireNonNull(bmp));
            Objects.requireNonNull(report.getSections().getFirst().getPhotos().getFirst().getAttachment())
                    .setContentType("image/bmp");

            byte[] pdf = generator.generate(report, tenant());

            assertSinglePageHealthyPdf(pdf);
        } finally {
            TimeZone.setDefault(previous);
        }
    }

    @Test
    void generate_allowsMultipleShortSectionsOnOnePageWhenRoomPermits() throws Exception {
        AttachmentStorageService storageService = mock(AttachmentStorageService.class);
        CustomerPhotoReportPdfGenerator generator = new CustomerPhotoReportPdfGenerator(storageService);

        Customer customer = new Customer();
        Job job = new Job();
        job.setCustomer(customer);
        CustomerPhotoReport report = new CustomerPhotoReport();
        report.setCustomer(customer);
        report.setJob(job);
        report.setTitle("Photo report");
        report.setSummary(null);

        for (int i = 0; i < 3; i++) {
            CustomerPhotoReportSection s = new CustomerPhotoReportSection();
            s.setSortOrder(i);
            s.setTitle("Section " + (i + 1));
            s.setBody(null);
            report.getSections().add(s);
        }

        byte[] pdf = generator.generate(report, tenant());
        try (PDDocument document = PDDocument.load(pdf)) {
            assertTrue(document.getNumberOfPages() == 1,
                    "Short sections should share a page when there is enough room.");
        }
    }

    @Test
    void estimateSectionStartMinHeight_includesPhotoBlockWhenPhotosPresent() throws Exception {
        AttachmentStorageService storageService = mock(AttachmentStorageService.class);
        CustomerPhotoReportPdfGenerator generator = new CustomerPhotoReportPdfGenerator(storageService);

        CustomerPhotoReportSection titleOnly = new CustomerPhotoReportSection();
        titleOnly.setTitle("Alpha");
        titleOnly.setBody(null);

        CustomerPhotoReportSection withPhoto = new CustomerPhotoReportSection();
        withPhoto.setTitle("Beta");
        withPhoto.setBody(null);
        Attachment att = new Attachment();
        CustomerPhotoReportSectionPhoto link = new CustomerPhotoReportSectionPhoto();
        link.setAttachment(att);
        withPhoto.getPhotos().add(link);

        float a = generator.estimateSectionStartMinHeight(titleOnly);
        float b = generator.estimateSectionStartMinHeight(withPhoto);
        assertTrue(b > a, "Sections with photos should reserve more vertical start space.");
    }

    @Test
    void generate_embedsTiffRasterWhenWriterAvailable() throws Exception {
        TimeZone previous = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/Denver"));
        try {
            byte[] tiff = tryWriteRasterAsFormat("tiff", 96, 72, Color.YELLOW);
            Assumptions.assumeTrue(tiff != null && tiff.length > 0);
            AttachmentStorageService storageService = mock(AttachmentStorageService.class);
            CustomerPhotoReportPdfGenerator generator = new CustomerPhotoReportPdfGenerator(storageService);
            CustomerPhotoReport report =
                    buildReportWithImage(storageService, "roof.tif", "report/roof.tif", tiff);
            Objects.requireNonNull(report.getSections().getFirst().getPhotos().getFirst().getAttachment())
                    .setContentType("image/tiff");

            byte[] pdf = generator.generate(report, tenant());

            assertSinglePageHealthyPdf(pdf);
        } finally {
            TimeZone.setDefault(previous);
        }
    }

    @Test
    void generate_embedsWebpRasterWhenWriterAvailable() throws Exception {
        TimeZone previous = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/Denver"));
        try {
            Assumptions.assumeTrue(Arrays.stream(ImageIO.getWriterFormatNames()).anyMatch(w -> w.equalsIgnoreCase("webp")));

            BufferedImage image = new BufferedImage(64, 48, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            try {
                g.setColor(Color.CYAN);
                g.fillRect(0, 0, 64, 48);
            } finally {
                g.dispose();
            }
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            Assumptions.assumeTrue(ImageIO.write(image, "webp", bout));
            byte[] webp = bout.toByteArray();

            AttachmentStorageService storageService = mock(AttachmentStorageService.class);
            CustomerPhotoReportPdfGenerator generator = new CustomerPhotoReportPdfGenerator(storageService);
            CustomerPhotoReport report =
                    buildReportWithImage(storageService, "photo.webp", "report/photo.webp", webp);
            Objects.requireNonNull(report.getSections().getFirst().getPhotos().getFirst().getAttachment())
                    .setContentType("image/webp");

            byte[] pdf = generator.generate(report, tenant());

            assertSinglePageHealthyPdf(pdf);
        } finally {
            TimeZone.setDefault(previous);
        }
    }

    @Test
    void generate_downscaledRasterKeepsPdfMuchSmallerThanRawBitmapPayload() throws Exception {
        TimeZone previous = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/Denver"));
        try {
            byte[] hugeBmp = tryWriteRasterAsFormat("bmp", 2200, 2200, Color.DARK_GRAY);
            Assumptions.assumeTrue(hugeBmp != null && hugeBmp.length > 2_000_000);
            byte[] hugeBmpData = Objects.requireNonNull(hugeBmp);

            AttachmentStorageService storageService = mock(AttachmentStorageService.class);
            CustomerPhotoReportPdfGenerator generator = new CustomerPhotoReportPdfGenerator(storageService);
            CustomerPhotoReport report =
                    buildReportWithImage(storageService, "big-roof.bmp", "report/big-roof.bmp", hugeBmpData);
            Objects.requireNonNull(report.getSections().getFirst().getPhotos().getFirst().getAttachment())
                    .setContentType("image/bmp");

            byte[] pdf = generator.generate(report, tenant());

            assertTrue(pdf.length < hugeBmpData.length / 10,
                    "Downscaled JPEG embed should dwarf uncompressed BMP payload");
            assertSinglePageHealthyPdf(pdf);
        } finally {
            TimeZone.setDefault(previous);
        }
    }

    @Test
    void generate_pdfPayloadStableAcrossLargeSourceDimensions() throws Exception {
        TimeZone previous = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/Denver"));
        try {
            byte[] bmpWide = tryWriteRasterAsFormat("bmp", 3000, 1000, Color.RED);
            byte[] bmpTall = tryWriteRasterAsFormat("bmp", 1000, 3000, Color.RED);
            Assumptions.assumeTrue(bmpWide != null && bmpTall != null);
            byte[] bmpWideData = Objects.requireNonNull(bmpWide);
            byte[] bmpTallData = Objects.requireNonNull(bmpTall);
            Assumptions.assumeTrue(bmpWideData.length > 1_500_000 && bmpTallData.length > 1_500_000);

            AttachmentStorageService storageService = mock(AttachmentStorageService.class);
            CustomerPhotoReportPdfGenerator generator = new CustomerPhotoReportPdfGenerator(storageService);

            CustomerPhotoReport wideReport =
                    buildReportWithImage(storageService, "w.bmp", "report/w.bmp", bmpWideData);
            Objects.requireNonNull(wideReport.getSections().getFirst().getPhotos().getFirst().getAttachment())
                    .setContentType("image/bmp");
            CustomerPhotoReport tallReport =
                    buildReportWithImage(storageService, "t.bmp", "report/t.bmp", bmpTallData);
            Objects.requireNonNull(tallReport.getSections().getFirst().getPhotos().getFirst().getAttachment())
                    .setContentType("image/bmp");

            byte[] pdfWide = generator.generate(wideReport, tenant());
            byte[] pdfTall = generator.generate(tallReport, tenant());

            assertTrue(Math.abs(pdfWide.length - pdfTall.length) < 250_000,
                    "Same visual cap implies similar embedded raster weight");
        } finally {
            TimeZone.setDefault(previous);
        }
    }

    @Test
    void downscaleForPdfEmbed_capsLongEdge() {
        BufferedImage huge = new BufferedImage(5000, 2000, BufferedImage.TYPE_INT_RGB);
        BufferedImage out = CustomerPhotoReportPdfGenerator.downscaleForPdfEmbed(huge);
        assertTrue(Math.max(out.getWidth(), out.getHeight()) <= CustomerPhotoReportPdfGenerator.MAX_EMBED_LONG_SIDE_PX);
    }

    @Test
    void downscaleForPdfEmbed_preservesSmallInstances() {
        BufferedImage small = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
        assertSame(small, CustomerPhotoReportPdfGenerator.downscaleForPdfEmbed(small));
    }

    @Test
    void needsDownscale_dependsOnLongEdgeVsCap() {
        assertFalse(CustomerPhotoReportPdfGenerator.needsDownscale(new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB)));
        assertTrue(CustomerPhotoReportPdfGenerator.needsDownscale(new BufferedImage(2000, 1500, BufferedImage.TYPE_INT_RGB)));
    }

    @Test
    void insufficientRemainingSpace_matchesFitRule() {
        assertTrue(CustomerPhotoReportPdfGenerator.insufficientRemainingSpace(100f, 84f, 50f));
        assertFalse(CustomerPhotoReportPdfGenerator.insufficientRemainingSpace(200f, 84f, 50f));
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

    private Tenant tenant() {
        Tenant tenant = new Tenant();
        tenant.setName("Acme Roofing");
        return tenant;
    }

    private static void assertSinglePageHealthyPdf(byte[] pdf) throws Exception {
        try (PDDocument document = PDDocument.load(pdf)) {
            assertTrue(document.getNumberOfPages() >= 1);
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Front elevation"));
            assertTrue(!text.contains("Photo 1.1"));
        }
    }

    private static byte[] tryWriteRasterAsFormat(String format, int width, int height, Color fill) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(fill);
            g.fillRect(0, 0, width, height);
        } finally {
            g.dispose();
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            boolean ok = ImageIO.write(image, format, out);
            if (!ok) {
                return null;
            }
            return out.toByteArray();
        }
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
