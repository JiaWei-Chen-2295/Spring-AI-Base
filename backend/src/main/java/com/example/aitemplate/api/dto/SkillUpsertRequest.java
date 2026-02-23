package com.example.aitemplate.api.dto;

import jakarta.validation.constraints.NotBlank;

public record SkillUpsertRequest(
        @NotBlank String skillName,
        String version,
        @NotBlank String content
) {
}
