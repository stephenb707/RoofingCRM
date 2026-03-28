package com.roofingcrm.service.mail;

import com.roofingcrm.domain.entity.Tenant;
import com.roofingcrm.domain.enums.UserRole;

public class InviteEmailTemplateBuilder {

    public EmailMessage build(Tenant tenant, String recipientEmail, UserRole role, String inviteUrl) {
        String senderName = tenant.getName() == null || tenant.getName().trim().isEmpty()
                ? "VivaCRM"
                : tenant.getName().trim();
        String roleLabel = toRoleLabel(role);
        String subject = "You're invited to join " + senderName + " on VivaCRM";

        String html = """
                <p>Hello,</p>
                <p>%s invited you to join their team on VivaCRM as <strong>%s</strong>.</p>
                <p><a href="%s">Join team</a></p>
                <p>If the button above does not work, copy and paste this URL into your browser:<br/>%s</p>
                <p>Thank you,<br/>VivaCRM</p>
                """.formatted(
                escapeHtml(senderName),
                escapeHtml(roleLabel),
                escapeHtml(inviteUrl),
                escapeHtml(inviteUrl)
        );

        String text = """
                Hello,

                %s invited you to join their team on VivaCRM as %s.

                Join team: %s

                Thank you,
                VivaCRM
                """.formatted(senderName, roleLabel, inviteUrl);

        return new EmailMessage(recipientEmail, subject, html, text);
    }

    private String toRoleLabel(UserRole role) {
        return switch (role) {
            case OWNER -> "Owner";
            case ADMIN -> "Admin";
            case SALES -> "Sales";
            case FIELD_TECH -> "Field Tech";
        };
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
