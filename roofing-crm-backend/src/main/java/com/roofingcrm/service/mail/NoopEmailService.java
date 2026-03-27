package com.roofingcrm.service.mail;

import com.roofingcrm.service.exception.MailConfigurationException;

public class NoopEmailService implements EmailService {

    private final String reason;

    public NoopEmailService(String reason) {
        this.reason = reason;
    }

    @Override
    public void send(EmailMessage message) {
        throw new MailConfigurationException(reason);
    }
}
