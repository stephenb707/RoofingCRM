package com.roofingcrm.api;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ThrowingController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void returnsFriendlyMessageForMaxUploadSizeExceeded() throws Exception {
        mockMvc.perform(get("/test/upload-limit"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value(413))
                .andExpect(jsonPath("$.error").value("Payload Too Large"))
                .andExpect(jsonPath("$.message").value("File is too large. Maximum upload size is 20MB."))
                .andExpect(jsonPath("$.path").value("/test/upload-limit"));
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/test/upload-limit")
        void uploadLimit() {
            throw new MaxUploadSizeExceededException(20L * 1024L * 1024L);
        }
    }
}
