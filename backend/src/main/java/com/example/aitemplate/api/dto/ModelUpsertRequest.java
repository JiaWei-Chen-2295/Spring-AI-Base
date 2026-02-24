package com.example.aitemplate.api.dto;

import com.example.aitemplate.core.model.CapabilitySet;
import jakarta.validation.constraints.NotBlank;

public record ModelUpsertRequest(
        @NotBlank String modelId,
        String provider,
        String displayName,
        @NotBlank String baseUrl,
        @NotBlank String apiKey,
        @NotBlank String modelName,
        CapabilitySet capabilities,
        Integer sortOrder) {}
