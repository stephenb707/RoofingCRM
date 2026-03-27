package com.roofingcrm.service.mail;

import com.roofingcrm.domain.entity.Invoice;
import com.roofingcrm.domain.entity.Tenant;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

public class InvoiceEmailTemplateBuilder {

    public EmailMessage build(Tenant tenant,
                              Invoice invoice,
                              String recipientEmail,
                              String recipientName,
                              String subjectOverride,
                              String customMessage,
                              String publicUrl) {
        String senderName = isBlank(tenant.getName()) ? "Your roofing team" : tenant.getName().trim();
        String subject = isBlank(subjectOverride)
                ? "Invoice " + invoice.getInvoiceNumber() + " from " + senderName
                : subjectOverride.trim();
        String greeting = isBlank(recipientName) ? "Hello," : "Hello " + recipientName.trim() + ",";
        String intro = isBlank(customMessage)
                ? "Please review your invoice using the secure link below."
                : customMessage.trim();
        String total = formatMoney(invoice.getTotal(), tenant.getDefaultCurrencyCode());
        String signature = senderName;

        String html = """
                <p>%s</p>
                <p>%s</p>
                <p><strong>Invoice:</strong> %s<br/><strong>Total:</strong> %s</p>
                <p><a href="%s">View Invoice</a></p>
                <p>If the button above does not work, copy and paste this URL into your browser:<br/>%s</p>
                <p>Thank you,<br/>%s</p>
                """.formatted(
                escapeHtml(greeting),
                escapeHtml(intro),
                escapeHtml(invoice.getInvoiceNumber()),
                escapeHtml(total),
                escapeHtml(publicUrl),
                escapeHtml(publicUrl),
                escapeHtml(signature)
        );

        String text = """
                %s

                %s

                Invoice: %s
                Total: %s

                View Invoice: %s

                Thank you,
                %s
                """.formatted(greeting, intro, invoice.getInvoiceNumber(), total, publicUrl, signature);

        return new EmailMessage(recipientEmail, subject, html, text);
    }

    private static String formatMoney(BigDecimal amount, String currencyCode) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
        String code = isBlank(currencyCode) ? "USD" : currencyCode.trim().toUpperCase(Locale.US);
        formatter.setCurrency(Currency.getInstance(code));
        return formatter.format(amount == null ? BigDecimal.ZERO : amount);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
