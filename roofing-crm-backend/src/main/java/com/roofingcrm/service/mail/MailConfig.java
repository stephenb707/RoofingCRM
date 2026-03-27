package com.roofingcrm.service.mail;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class MailConfig {

    @Bean
    EmailService emailService(RestClient.Builder restClientBuilder, MailProperties mailProperties) {
        if (!mailProperties.isEnabled()) {
            return new NoopEmailService("Email sending is disabled. Set APP_MAIL_ENABLED=true to enable it.");
        }

        String provider = mailProperties.getProvider() == null ? "" : mailProperties.getProvider().trim().toLowerCase();
        if (!"resend".equals(provider)) {
            return new NoopEmailService("Email provider is not supported. Set APP_MAIL_PROVIDER=resend.");
        }

        if (isBlank(mailProperties.getFromEmail())) {
            return new NoopEmailService("Email sending is enabled but APP_MAIL_FROM_EMAIL is missing.");
        }

        if (isBlank(mailProperties.getResend().getApiKey())) {
            return new NoopEmailService("Email sending is enabled but APP_MAIL_RESEND_API_KEY is missing.");
        }

        return new ResendEmailService(restClientBuilder, mailProperties);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
