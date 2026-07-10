package com.roofingcrm.integrations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Encrypts integration secret payloads (UTF-8 JSON) with AES-256-GCM. Key material is derived with SHA-256
 * from {@link IntegrationEncryptionProperties#getEncryptionKey()}.
 */
public class AesGcmIntegrationSecretProtector implements IntegrationSecretProtector {

    private static final Logger log = LoggerFactory.getLogger(AesGcmIntegrationSecretProtector.class);
    private static final String PREFIX = "v1:";
    private final SecretKey secretKey;
    private final SecureRandom random = new SecureRandom();

    public AesGcmIntegrationSecretProtector(IntegrationEncryptionProperties properties) {
        String raw = properties.getEncryptionKey();
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("Integration encryption key is blank");
        }
        byte[] keyBytes;
        try {
            keyBytes = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        Arrays.fill(keyBytes, (byte) 0);
    }

    @Override
    public String protect(String plaintextUtf8) {
        try {
            byte[] iv = new byte[12];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
            byte[] ct = cipher.doFinal(plaintextUtf8.getBytes(StandardCharsets.UTF_8));
            return PREFIX + Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(ct);
        } catch (Exception ex) {
            log.error("Failed to encrypt integration secret payload");
            throw new IllegalStateException("Integration secret encryption failed", ex);
        }
    }

    @Override
    public String unprotect(String ciphertext) {
        if (ciphertext == null || !ciphertext.startsWith(PREFIX)) {
            throw new IllegalArgumentException("Unrecognized ciphertext");
        }
        try {
            String rest = ciphertext.substring(PREFIX.length());
            int colon = rest.indexOf(':');
            if (colon <= 0) {
                throw new IllegalArgumentException("Malformed ciphertext");
            }
            byte[] iv = Base64.getDecoder().decode(rest.substring(0, colon));
            byte[] ct = Base64.getDecoder().decode(rest.substring(colon + 1));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
            byte[] plain = cipher.doFinal(ct);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to decrypt integration secrets", ex);
        }
    }
}
