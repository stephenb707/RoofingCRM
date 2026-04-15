package com.roofingcrm.service.mail;

public record EmailAttachment(
        String filename,
        String content,
        String contentType
) {
}
