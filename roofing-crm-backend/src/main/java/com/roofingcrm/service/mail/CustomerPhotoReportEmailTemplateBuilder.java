package com.roofingcrm.service.mail;

import com.roofingcrm.domain.entity.CustomerPhotoReport;
import com.roofingcrm.domain.entity.Tenant;

import java.util.List;

public class CustomerPhotoReportEmailTemplateBuilder {

    public EmailMessage build(Tenant tenant,
                              CustomerPhotoReport report,
                              String recipientEmail,
                              String recipientName,
                              String subjectOverride,
                              String customMessage,
                              EmailAttachment attachment) {
        String senderName = isBlank(tenant.getName()) ? "Your roofing team" : tenant.getName().trim();
        String subject = isBlank(subjectOverride)
                ? senderName + " - " + fallbackTitle(report.getTitle())
                : subjectOverride.trim();
        String greeting = isBlank(recipientName) ? "Hello," : "Hello " + recipientName.trim() + ",";
        String intro = isBlank(customMessage)
                ? "Attached is your customer photo report for review."
                : customMessage.trim();

        String html = """
                <p>%s</p>
                <p>%s</p>
                <p><strong>Report:</strong> %s</p>
                <p>The PDF report is attached to this email.</p>
                <p>Thank you,<br/>%s</p>
                """.formatted(
                escapeHtml(greeting),
                escapeHtml(intro),
                escapeHtml(fallbackTitle(report.getTitle())),
                escapeHtml(senderName)
        );

        String text = """
                %s

                %s

                Report: %s
                The PDF report is attached to this email.

                Thank you,
                %s
                """.formatted(greeting, intro, fallbackTitle(report.getTitle()), senderName);

        return new EmailMessage(recipientEmail, subject, html, text, List.of(attachment));
    }

    private static String fallbackTitle(String title) {
        return isBlank(title) ? "Customer photo report" : title.trim();
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
