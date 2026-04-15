package com.roofingcrm.service.mail;

import com.roofingcrm.service.exception.MailDeliveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ResendEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailService.class);

    private final RestClient restClient;
    private final MailProperties mailProperties;

    public ResendEmailService(RestClient.Builder restClientBuilder, MailProperties mailProperties) {
        this.restClient = restClientBuilder.baseUrl("https://api.resend.com").build();
        this.mailProperties = mailProperties;
    }

    @Override
    public void send(EmailMessage message) {
        String fromName = blankToNull(mailProperties.getFromName());
        String fromEmail = blankToNull(mailProperties.getFromEmail());
        String from = fromName != null ? fromName + " <" + fromEmail + ">" : String.valueOf(fromEmail);

        Map<String, Object> payload = Map.of(
                "from", from,
                "to", List.of(message.toEmail()),
                "subject", message.subject(),
                "html", message.html(),
                "text", message.text()
        );
        if (message.attachments() != null && !message.attachments().isEmpty()) {
            payload = new java.util.HashMap<>(payload);
            payload.put("attachments", message.attachments().stream().map(attachment -> {
                Map<String, Object> entry = new java.util.HashMap<>();
                entry.put("filename", attachment.filename());
                entry.put("content", attachment.content());
                if (attachment.contentType() != null && !attachment.contentType().isBlank()) {
                    entry.put("content_type", attachment.contentType());
                }
                return entry;
            }).toList());
        }

        try {
            restClient.post()
                    .uri("/emails")
                    .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                    .header("Authorization", "Bearer " + mailProperties.getResend().getApiKey())
                    .body(Objects.requireNonNull(payload))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Sent transactional email via Resend to {}", message.toEmail());
        } catch (RestClientException ex) {
            log.error("Failed to send transactional email via Resend to {}", message.toEmail(), ex);
            throw new MailDeliveryException("Failed to send email through Resend.", ex);
        }
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
