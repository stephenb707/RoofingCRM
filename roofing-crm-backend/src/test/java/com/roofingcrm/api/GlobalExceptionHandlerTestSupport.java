package com.roofingcrm.api;

import com.roofingcrm.service.attachment.AttachmentUploadProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Pulls in {@link GlobalExceptionHandler} plus the {@link AttachmentUploadProperties}
 * bean required by its constructor, for {@code @WebMvcTest} slices.
 */
@Configuration
@EnableConfigurationProperties(AttachmentUploadProperties.class)
@Import(GlobalExceptionHandler.class)
public class GlobalExceptionHandlerTestSupport {
}
