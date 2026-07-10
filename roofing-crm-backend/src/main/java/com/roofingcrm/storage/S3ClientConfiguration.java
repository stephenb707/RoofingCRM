package com.roofingcrm.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
public class S3ClientConfiguration {

    @Bean
    @ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
    S3Client s3Client(S3AttachmentStorageProperties props) {
        if (props.getBucket() == null || props.getBucket().isBlank()) {
            throw new IllegalStateException("app.storage.s3.bucket is required when app.storage.provider=s3");
        }
        if (props.getAccessKey() == null || props.getAccessKey().isBlank()
                || props.getSecretKey() == null || props.getSecretKey().isBlank()) {
            throw new IllegalStateException("app.storage.s3 access credentials are required when app.storage.provider=s3");
        }
        var creds = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey()));
        var s3Conf = S3Configuration.builder()
                .pathStyleAccessEnabled(props.isPathStyleAccess())
                .build();
        var builder = S3Client.builder()
                .region(Region.of(props.getRegion() != null && !props.getRegion().isBlank() ? props.getRegion() : "us-east-1"))
                .credentialsProvider(creds)
                .serviceConfiguration(s3Conf);
        String endpoint = props.getEndpoint();
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint.trim()));
        }
        return builder.build();
    }
}
