package com.roofingcrm.service.mail;

import java.util.List;

public record EmailMessage(
        String toEmail,
        String subject,
        String html,
        String text,
        List<EmailAttachment> attachments
) {
    public EmailMessage(String toEmail, String subject, String html, String text) {
        this(toEmail, subject, html, text, List.of());
    }
}
