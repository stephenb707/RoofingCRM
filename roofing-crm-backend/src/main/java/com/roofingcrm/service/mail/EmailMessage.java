package com.roofingcrm.service.mail;

public record EmailMessage(
        String toEmail,
        String subject,
        String html,
        String text
) {
}
