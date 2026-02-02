package com.roofingcrm.api.publicapi.estimate;

import com.roofingcrm.service.estimate.PublicEstimateService;
import jakarta.validation.Valid;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/estimates")
public class PublicEstimateController {

    private final PublicEstimateService publicEstimateService;

    @Autowired
    public PublicEstimateController(PublicEstimateService publicEstimateService) {
        this.publicEstimateService = publicEstimateService;
    }

    @GetMapping("/{token}")
    public ResponseEntity<PublicEstimateDto> getByToken(@PathVariable("token") @NonNull String token) {
        PublicEstimateDto dto = publicEstimateService.getByToken(token);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{token}/decision")
    public ResponseEntity<PublicEstimateDto> decide(
            @PathVariable("token") @NonNull String token,
            @Valid @RequestBody PublicEstimateDecisionRequest request) {
        PublicEstimateDto dto = publicEstimateService.decide(token, Objects.requireNonNull(request));
        return ResponseEntity.ok(dto);
    }
}
