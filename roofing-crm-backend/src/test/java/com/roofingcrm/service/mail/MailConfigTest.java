package com.roofingcrm.service.mail;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.RETURNS_SELF;

class MailConfigTest {

    private final MailConfig mailConfig = new MailConfig();

    @Test
    void emailService_whenDisabled_returnsNoopService() {
        MailProperties properties = new MailProperties();

        EmailService emailService = mailConfig.emailService(mock(RestClient.Builder.class, RETURNS_SELF), properties);

        assertInstanceOf(NoopEmailService.class, emailService);
    }

    @Test
    void emailService_whenEnabledWithResendConfig_returnsResendService() {
        MailProperties properties = new MailProperties();
        properties.setEnabled(true);
        properties.setProvider("resend");
        properties.setFromEmail("noreply@example.com");
        properties.setFromName("Roofing CRM");
        properties.getResend().setApiKey("test-key");

        RestClient.Builder builder = mock(RestClient.Builder.class, RETURNS_SELF);
        when(builder.build()).thenReturn(mock(RestClient.class));

        EmailService emailService = mailConfig.emailService(builder, properties);

        assertInstanceOf(ResendEmailService.class, emailService);
    }

    @Test
    void emailService_whenEnabledWithoutApiKey_returnsNoopService() {
        MailProperties properties = new MailProperties();
        properties.setEnabled(true);
        properties.setProvider("resend");
        properties.setFromEmail("noreply@example.com");

        EmailService emailService = mailConfig.emailService(mock(RestClient.Builder.class, RETURNS_SELF), properties);

        assertInstanceOf(NoopEmailService.class, emailService);
    }
}
