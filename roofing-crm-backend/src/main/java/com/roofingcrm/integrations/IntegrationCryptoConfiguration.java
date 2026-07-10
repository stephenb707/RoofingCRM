package com.roofingcrm.integrations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IntegrationCryptoConfiguration {

    @Bean
    IntegrationSecretProtector integrationSecretProtector(IntegrationEncryptionProperties properties) {
        if (properties.getEncryptionKey() != null && !properties.getEncryptionKey().isBlank()) {
            return new AesGcmIntegrationSecretProtector(properties);
        }
        return new DevelopmentIntegrationSecretProtector();
    }
}
