package com.roofingcrm.service.report;

import com.roofingcrm.domain.entity.Invoice;
import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.value.Address;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
public class PaidInvoicesPdfGenerator {

    private static final float MARGIN = 40f;
    private static final float ROW_HEIGHT = 16f;
    private static final float TITLE_SIZE = 16f;
    private static final float HEADER_SIZE = 10f;
    private static final float BODY_SIZE = 9f;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);
    private static final NumberFormat MONEY_FMT = NumberFormat.getCurrencyInstance(Locale.US);

    public byte[] generate(Tenant tenant, int year, List<Invoice> invoices) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            float y = page.getMediaBox().getHeight() - MARGIN;
            BigDecimal totalPaid = BigDecimal.ZERO;
            PDPageContentStream content = new PDPageContentStream(document, page);
            try {
                y = drawPageHeader(content, year, tenant != null ? tenant.getName() : null, y);
                y = drawTableHeader(content, y);

                for (Invoice invoice : invoices) {
                    totalPaid = totalPaid.add(invoice.getTotal() != null ? invoice.getTotal() : BigDecimal.ZERO);
                    if (y < MARGIN + 70f) {
                        content.close();
                        page = new PDPage(PDRectangle.LETTER);
                        document.addPage(page);
                        content = new PDPageContentStream(document, page);
                        y = page.getMediaBox().getHeight() - MARGIN;
                        y = drawPageHeader(content, year, tenant != null ? tenant.getName() : null, y);
                        y = drawTableHeader(content, y);
                    }
                    y = drawInvoiceRow(content, invoice, y);
                    y -= 2f;
                }

                y -= 6f;
                drawLine(content, y + 8f);
                drawText(content, "Total paid: " + MONEY_FMT.format(totalPaid), MARGIN, y - 6f, PDType1Font.HELVETICA_BOLD, 11f);
            } finally {
                content.close();
            }
            return save(document);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate paid invoices PDF", ex);
        }
    }

    private float drawPageHeader(PDPageContentStream content, int year, String tenantName, float y) throws IOException {
        drawText(content, "Paid Invoices Report — " + year, MARGIN, y, PDType1Font.HELVETICA_BOLD, TITLE_SIZE);
        y -= 20f;
        if (tenantName != null && !tenantName.isBlank()) {
            drawText(content, "Tenant: " + tenantName, MARGIN, y, PDType1Font.HELVETICA, HEADER_SIZE);
            y -= 14f;
        }
        drawText(content, "Generated: " + TS_FMT.format(Instant.now()), MARGIN, y, PDType1Font.HELVETICA, HEADER_SIZE);
        return y - 18f;
    }

    private float drawTableHeader(PDPageContentStream content, float y) throws IOException {
        drawLine(content, y + 6f);
        drawText(content, "Paid Date", MARGIN, y - 2f, PDType1Font.HELVETICA_BOLD, HEADER_SIZE);
        drawText(content, "Invoice #", 110f, y - 2f, PDType1Font.HELVETICA_BOLD, HEADER_SIZE);
        drawText(content, "Customer", 180f, y - 2f, PDType1Font.HELVETICA_BOLD, HEADER_SIZE);
        drawText(content, "Job Address", 300f, y - 2f, PDType1Font.HELVETICA_BOLD, HEADER_SIZE);
        drawText(content, "Total", 520f, y - 2f, PDType1Font.HELVETICA_BOLD, HEADER_SIZE);
        drawLine(content, y - 8f);
        return y - 20f;
    }

    private float drawInvoiceRow(PDPageContentStream content, Invoice invoice, float y) throws IOException {
        String paidDate = invoice.getPaidAt() != null ? DATE_FMT.format(invoice.getPaidAt()) : "—";
        String invoiceNumber = nonBlank(invoice.getInvoiceNumber(), "—");
        String customer = "—";
        if (invoice.getJob() != null && invoice.getJob().getCustomer() != null) {
            String first = invoice.getJob().getCustomer().getFirstName();
            String last = invoice.getJob().getCustomer().getLastName();
            String name = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
            customer = nonBlank(name, "—");
        }
        String address = "—";
        if (invoice.getJob() != null) {
            address = nonBlank(formatAddress(invoice.getJob().getPropertyAddress()), "—");
        }
        String total = MONEY_FMT.format(invoice.getTotal() != null ? invoice.getTotal() : BigDecimal.ZERO);

        drawText(content, truncate(paidDate, 12), MARGIN, y, PDType1Font.HELVETICA, BODY_SIZE);
        drawText(content, truncate(invoiceNumber, 12), 110f, y, PDType1Font.HELVETICA, BODY_SIZE);
        drawText(content, truncate(customer, 22), 180f, y, PDType1Font.HELVETICA, BODY_SIZE);
        drawText(content, truncate(address, 38), 300f, y, PDType1Font.HELVETICA, BODY_SIZE);
        drawText(content, truncate(total, 12), 520f, y, PDType1Font.HELVETICA, BODY_SIZE);
        return y - ROW_HEIGHT;
    }

    private static String formatAddress(Address address) {
        if (address == null) return "";
        StringBuilder sb = new StringBuilder();
        appendPart(sb, address.getLine1());
        appendPart(sb, address.getLine2());
        appendPart(sb, address.getCity());
        String stateZip = ((address.getState() != null ? address.getState().trim() : "") + " " + (address.getZip() != null ? address.getZip().trim() : "")).trim();
        appendPart(sb, stateZip);
        return sb.toString();
    }

    private static void appendPart(StringBuilder sb, String part) {
        if (part == null || part.isBlank()) return;
        if (!sb.isEmpty()) sb.append(", ");
        sb.append(part.trim());
    }

    private static String truncate(String value, int max) {
        if (value == null) return "";
        if (value.length() <= max) return value;
        return value.substring(0, Math.max(0, max - 1)) + "…";
    }

    private static String nonBlank(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private static void drawLine(PDPageContentStream content, float y) throws IOException {
        content.moveTo(MARGIN, y);
        content.lineTo(PDRectangle.LETTER.getWidth() - MARGIN, y);
        content.stroke();
    }

    private static void drawText(PDPageContentStream content, String text, float x, float y,
                                 PDFont font, float size) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(text != null ? text : "");
        content.endText();
    }

    private static byte[] save(PDDocument document) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.save(out);
            return out.toByteArray();
        }
    }
}
