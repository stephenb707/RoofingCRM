package com.roofingcrm;

import com.roofingcrm.security.RefreshTokenProperties;
import com.roofingcrm.security.ratelimit.RateLimitProperties;
import com.roofingcrm.service.attachment.AttachmentUploadProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({AttachmentUploadProperties.class, RateLimitProperties.class, RefreshTokenProperties.class})
public class RoofingCrmApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoofingCrmApplication.class, args);
    }
}
