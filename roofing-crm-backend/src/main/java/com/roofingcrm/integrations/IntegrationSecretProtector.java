package com.roofingcrm.integrations;

public interface IntegrationSecretProtector {

    String protect(String plaintextUtf8);

    String unprotect(String ciphertext) throws IllegalArgumentException;
}
