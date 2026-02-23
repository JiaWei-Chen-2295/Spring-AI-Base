package com.example.aitemplate.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ChatRequest(
        @NotBlank String conversationId,
        @NotBlank String modelId,
        @NotBlank String message,
        List<String> tools,
        List<String> skills
) {}
