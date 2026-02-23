package com.example.aitemplate.core.chat;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ChatCommand(
        @NotBlank String conversationId,
        @NotBlank String modelId,
        @NotBlank String message,
        List<String> tools,
        List<String> skills
) {}
