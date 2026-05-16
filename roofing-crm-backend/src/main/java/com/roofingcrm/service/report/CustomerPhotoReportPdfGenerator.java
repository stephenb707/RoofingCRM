package com.roofingcrm.service.report;

import com.roofingcrm.domain.entity.Attachment;
import com.roofingcrm.domain.entity.CustomerPhotoReport;
import com.roofingcrm.domain.entity.CustomerPhotoReportSection;
import com.roofingcrm.domain.entity.CustomerPhotoReportSectionPhoto;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.service.customerreport.CustomerPhotoReportPresentationHelper;
import com.roofingcrm.storage.AttachmentStorageService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class CustomerPhotoReportPdfGenerator {

    private static final float MARGIN = 48f;
    private static final float MIN_Y = MARGIN + 36f;
    private static final float CONTENT_WIDTH = PDRectangle.LETTER.getWidth() - (2 * MARGIN);
    private static final float TITLE_SIZE = 18f;
    private static final float SUBTITLE_SIZE = 14f;
    private static final float HEADING_SIZE = 12f;
    private static final float BODY_SIZE = 10f;
    private static final float LINE_LEADING = 13f;
    private static final float SECTION_SPACING = 12f;
    private static final float IMAGE_BOTTOM_PADDING = 10f;
    private static final float IMAGE_TOP_PADDING = 8f;
    /** Max drawable width/heights so section text + multiple photos fit on a page more often. */
    private static final float MAX_IMAGE_DISPLAY_WIDTH_PT = CONTENT_WIDTH * 0.78f;
    private static final float MAX_IMAGE_DISPLAY_HEIGHT_PT = 200f;
    /**
     * Minimum vertical space (pt) we want free below the cursor before starting a section
     * (section heading + a bit of body and/or first photo slot). Avoids orphaned headings.
     */
    private static final float MIN_SECTION_START_FLOOR_PT = 72f;
    /** Reasonable display height budget for "first photo" when estimating section-start fit. pt */
    private static final float FIRST_PHOTO_BLOCK_ESTIMATE_PT = 140f;

    /**
     * Cap longest edge (px) for decoded raster before embedding. On-page layout uses smaller dimensions;
     * this bounds decode cost and PDF payload size for multi-megapixel phone photos.
     */
    static final int MAX_EMBED_LONG_SIDE_PX = 1400;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US);

    private final AttachmentStorageService storageService;

    public CustomerPhotoReportPdfGenerator(AttachmentStorageService storageService) {
        this.storageService = storageService;
    }

    public byte[] generate(CustomerPhotoReport report, Tenant tenant) {
        try (PDDocument document = new PDDocument()) {
            PageState state = new PageState(document);
            state.y = drawHeader(state, report, tenant);
            state.y -= SECTION_SPACING;

            List<CustomerPhotoReportSection> sections = report.getSections() != null
                    ? new ArrayList<>(report.getSections())
                    : List.of();
            sections.sort((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()));

            for (CustomerPhotoReportSection section : sections) {
                ensureRoomForSectionStart(state, section);

                state.y = writeBlock(state,
                        section.getTitle() != null ? section.getTitle() : "Section",
                        PDType1Font.HELVETICA_BOLD,
                        HEADING_SIZE,
                        HEADING_SIZE + 6f);

                if (section.getBody() != null && !section.getBody().isBlank()) {
                    state.y = writeParagraphs(state, section.getBody().trim(), PDType1Font.HELVETICA, BODY_SIZE, LINE_LEADING);
                }

                List<CustomerPhotoReportSectionPhoto> photos = section.getPhotos() != null
                        ? new ArrayList<>(section.getPhotos())
                        : List.of();
                photos.sort((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()));

                for (CustomerPhotoReportSectionPhoto link : photos) {
                    state.y = drawPhoto(state, link.getAttachment());
                }
                state.y -= SECTION_SPACING;
            }

            state.content.close();
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                document.save(out);
                return out.toByteArray();
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate customer photo report PDF", ex);
        }
    }

    private float drawHeader(PageState state, CustomerPhotoReport report, Tenant tenant) throws IOException {
        String tenantLine = tenant != null && tenant.getName() != null && !tenant.getName().isBlank()
                ? tenant.getName().trim()
                : "Roofing report";
        float y = writeBlock(state, tenantLine, PDType1Font.HELVETICA_BOLD, TITLE_SIZE, TITLE_SIZE + 8f);
        state.y = y;
        y = writeBlock(state, report.getTitle() != null ? report.getTitle() : "Report",
                PDType1Font.HELVETICA_BOLD, SUBTITLE_SIZE, SUBTITLE_SIZE + 6f);
        state.y = y;

        String customerName = CustomerPhotoReportPresentationHelper.formatCustomerName(report.getCustomer());
        if (!customerName.isBlank()) {
            y = writeBlock(state, "Prepared for: " + customerName, PDType1Font.HELVETICA, BODY_SIZE, LINE_LEADING);
            state.y = y;
        }

        if (report.getJob() != null) {
            String jobLine = "Related job: " + CustomerPhotoReportPresentationHelper.formatJobDisplay(report.getJob());
            y = writeBlock(state, jobLine, PDType1Font.HELVETICA, BODY_SIZE, LINE_LEADING);
            state.y = y;
        }

        if (report.getReportType() != null && !report.getReportType().isBlank()) {
            y = writeBlock(state, "Report type: " + report.getReportType(), PDType1Font.HELVETICA, BODY_SIZE, LINE_LEADING);
            state.y = y;
        }

        LocalDate reportDate = CustomerPhotoReportPresentationHelper.resolveReportLocalDate(report);
        y = writeBlock(state, "Date: " + DATE_FMT.format(reportDate), PDType1Font.HELVETICA, BODY_SIZE, LINE_LEADING);
        state.y = y;

        if (report.getSummary() != null && !report.getSummary().isBlank()) {
            y -= 4f;
            state.y = y;
            y = writeBlock(state, "Summary", PDType1Font.HELVETICA_BOLD, HEADING_SIZE, HEADING_SIZE + 4f);
            state.y = y;
            y = writeParagraphs(state, report.getSummary().trim(), PDType1Font.HELVETICA, BODY_SIZE, LINE_LEADING);
        }

        state.y = y;
        return y;
    }

    private float drawPhoto(PageState state, Attachment attachment) throws IOException {
        if (attachment == null || attachment.getStorageKey() == null || attachment.getStorageKey().isBlank()) {
            return writeBlock(state, "(Photo unavailable)", PDType1Font.HELVETICA_OBLIQUE, BODY_SIZE, LINE_LEADING + 6f);
        }

        byte[] bytes;
        try (InputStream in = storageService.loadAsStream(attachment.getStorageKey())) {
            bytes = in.readAllBytes();
        } catch (Exception ex) {
            return writeBlock(state, "(Could not load photo file)", PDType1Font.HELVETICA_OBLIQUE, BODY_SIZE, LINE_LEADING + 6f);
        }

        PDImageXObject img = toImage(state.document, bytes, attachment.getContentType());
        if (img == null) {
            return writeBlock(state, "(Unsupported image format)", PDType1Font.HELVETICA_OBLIQUE, BODY_SIZE, LINE_LEADING + 6f);
        }

        float natW = Math.max(1f, img.getWidth());
        float natH = Math.max(1f, img.getHeight());
        float scale = Math.min(1f, MAX_IMAGE_DISPLAY_WIDTH_PT / natW);
        scale = Math.min(scale, MAX_IMAGE_DISPLAY_HEIGHT_PT / natH);
        float dispW = natW * scale;
        float dispH = natH * scale;

        state.y = ensureSpace(state, dispH + IMAGE_TOP_PADDING + IMAGE_BOTTOM_PADDING);
        float drawY = state.y - IMAGE_TOP_PADDING - dispH;
        state.content.drawImage(img, MARGIN, drawY, dispW, dispH);
        return drawY - IMAGE_BOTTOM_PADDING;
    }

    /**
     * If too little room remains for this section's opening block (title + initial content chunk),
     * start the section on the next page. Otherwise continue on the current page so multiple
     * short sections can share a page.
     */
    private void ensureRoomForSectionStart(PageState state, CustomerPhotoReportSection section) throws IOException {
        float requiredBelowCursor = estimateSectionStartMinHeight(section);
        if (insufficientRemainingSpace(state.y, MIN_Y, requiredBelowCursor)) {
            beginSectionOnFreshPage(state);
        }
    }

    /**
     * Rough minimum height needed below the cursor to begin a section without cramming the heading
     * against the footer: wrapped section title lines, a small body chunk, and/or first photo block.
     */
    float estimateSectionStartMinHeight(CustomerPhotoReportSection section) throws IOException {
        String titleText = section.getTitle() != null && !section.getTitle().isBlank()
                ? section.getTitle()
                : "Section";
        float lineAdvance = HEADING_SIZE + 6f;
        int titleLines = wrapParagraph(titleText, PDType1Font.HELVETICA_BOLD, HEADING_SIZE, CONTENT_WIDTH).size();
        float h = titleLines * lineAdvance;
        h += 6f;
        if (section.getBody() != null && !section.getBody().isBlank()) {
            h += LINE_LEADING * 2;
        }
        List<CustomerPhotoReportSectionPhoto> photos = section.getPhotos();
        if (photos != null && !photos.isEmpty()) {
            float photoH = Math.min(MAX_IMAGE_DISPLAY_HEIGHT_PT, FIRST_PHOTO_BLOCK_ESTIMATE_PT);
            h += IMAGE_TOP_PADDING + photoH + IMAGE_BOTTOM_PADDING;
        }
        return Math.max(MIN_SECTION_START_FLOOR_PT, h);
    }

    /**
     * @return true when the vertical gap between the cursor and the page footer is less than required
     */
    static boolean insufficientRemainingSpace(float cursorY, float pageContentFloorY, float requiredBelowCursor) {
        return cursorY - pageContentFloorY < requiredBelowCursor;
    }

    private void beginSectionOnFreshPage(PageState state) throws IOException {
        state.content.close();
        PDPage page = new PDPage(PDRectangle.LETTER);
        state.document.addPage(page);
        state.content = new PDPageContentStream(state.document, page);
        state.y = page.getMediaBox().getHeight() - MARGIN;
    }

    private PDImageXObject toImage(PDDocument doc, byte[] data, String contentType) throws IOException {
        String ct = contentType != null ? contentType.toLowerCase(Locale.ROOT) : "";
        BufferedImage bi = ImageIO.read(new ByteArrayInputStream(data));
        if (bi != null) {
            if (!needsDownscale(bi)) {
                if (isJpegContentType(ct)) {
                    try {
                        return JPEGFactory.createFromByteArray(doc, data);
                    } catch (IOException ignored) {
                        // Mislabeled bytes or formats ImageIO handled but PDFBox JPEG rejects — embed from raster.
                    }
                }
                return losslessPdfImage(doc, bi);
            }
            BufferedImage scaled = downscaleForPdfEmbed(bi);
            if (scaled.getColorModel().hasAlpha()) {
                return losslessPdfImage(doc, scaled);
            }
            return embedOpaqueAsJpeg(doc, scaled);
        }
        if (isJpegContentType(ct)) {
            try {
                return JPEGFactory.createFromByteArray(doc, data);
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    static boolean needsDownscale(BufferedImage bi) {
        return Math.max(bi.getWidth(), bi.getHeight()) > MAX_EMBED_LONG_SIDE_PX;
    }

    static BufferedImage downscaleForPdfEmbed(BufferedImage src) {
        int maxSide = Math.max(src.getWidth(), src.getHeight());
        if (maxSide <= MAX_EMBED_LONG_SIDE_PX) {
            return src;
        }
        double scale = MAX_EMBED_LONG_SIDE_PX / (double) maxSide;
        int nw = Math.max(1, (int) Math.round(src.getWidth() * scale));
        int nh = Math.max(1, (int) Math.round(src.getHeight() * scale));
        int imageType = src.getColorModel().hasAlpha()
                ? BufferedImage.TYPE_INT_ARGB
                : BufferedImage.TYPE_INT_RGB;
        BufferedImage dst = new BufferedImage(nw, nh, imageType);
        Graphics2D g = dst.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            if (!src.getColorModel().hasAlpha()) {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, nw, nh);
            }
            g.drawImage(src, 0, 0, nw, nh, null);
        } finally {
            g.dispose();
        }
        return dst;
    }

    private static boolean isJpegContentType(String ctLowercase) {
        return ctLowercase.contains("jpeg") || ctLowercase.contains("jpg") || ctLowercase.contains("pjpeg");
    }

    private PDImageXObject embedOpaqueAsJpeg(PDDocument doc, BufferedImage bi) throws IOException {
        BufferedImage rgb = ensureRgbForJpeg(bi);
        try {
            return JPEGFactory.createFromImage(doc, rgb);
        } catch (Exception ex) {
            return losslessPdfImage(doc, rgb);
        }
    }

    private static BufferedImage ensureRgbForJpeg(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) {
            return src;
        }
        return copyToRgb(src);
    }

    private PDImageXObject losslessPdfImage(PDDocument doc, BufferedImage bi) throws IOException {
        try {
            return LosslessFactory.createFromImage(doc, bi);
        } catch (Exception ex) {
            BufferedImage rgb = copyToRgb(bi);
            return LosslessFactory.createFromImage(doc, rgb);
        }
    }

    private static BufferedImage copyToRgb(BufferedImage src) {
        BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            g.drawImage(src, 0, 0, null);
        } finally {
            g.dispose();
        }
        return rgb;
    }

    private float ensureSpace(PageState state, float neededHeight) throws IOException {
        if (state.y - neededHeight < MIN_Y) {
            state.content.close();
            PDPage page = new PDPage(PDRectangle.LETTER);
            state.document.addPage(page);
            state.content = new PDPageContentStream(state.document, page);
            state.y = page.getMediaBox().getHeight() - MARGIN;
        }
        return state.y;
    }

    private float writeParagraphs(PageState state, String paragraph, PDFont font, float size, float lineHeight) throws IOException {
        float y = state.y;
        for (String line : wrapParagraph(paragraph, font, size, CONTENT_WIDTH)) {
            y = writeBlock(state, line, font, size, lineHeight);
            state.y = y;
        }
        return y;
    }

    private float writeBlock(PageState state, String text, PDFont font, float size, float lineHeight) throws IOException {
        float y = state.y;
        for (String line : wrapParagraph(text, font, size, CONTENT_WIDTH)) {
            y = ensureSpace(state, lineHeight);
            drawText(state.content, line, MARGIN, y, font, size);
            y -= lineHeight;
            state.y = y;
        }
        return y;
    }

    private static void drawText(PDPageContentStream content, String text, float x, float y,
                                 PDFont font, float size) throws IOException {
        String safe = sanitizeOneLine(text);
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(safe);
        content.endText();
    }

    private static String sanitizeOneLine(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.replace('\r', ' ').replace('\n', ' ').trim();
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append(c <= 255 && c != '\r' && c != '\n' ? c : '?');
        }
        return sb.toString();
    }

    private static List<String> wrapParagraph(String paragraph, PDFont font, float size, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        String[] hardBreaks = paragraph.split("\\r?\\n");
        for (String chunk : hardBreaks) {
            if (chunk.isBlank()) {
                lines.add("");
                continue;
            }
            String[] words = chunk.trim().split("\\s+");
            StringBuilder current = new StringBuilder();
            for (String w : words) {
                if (current.isEmpty()) {
                    current.append(w);
                } else if (stringWidth(font, size, current + " " + w) <= maxWidth) {
                    current.append(' ').append(w);
                } else {
                    lines.add(current.toString());
                    current = new StringBuilder(w);
                }
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
            }
        }
        return lines;
    }

    private static float stringWidth(PDFont font, float size, String text) throws IOException {
        return font.getStringWidth(sanitizeOneLine(text)) / 1000f * size;
    }

    private static final class PageState {
        final PDDocument document;
        PDPageContentStream content;
        float y;

        PageState(PDDocument document) throws IOException {
            this.document = document;
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            this.content = new PDPageContentStream(document, page);
            this.y = page.getMediaBox().getHeight() - MARGIN;
        }
    }
}
