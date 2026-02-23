package com.example.aitemplate.api.dto;

import jakarta.validation.constraints.NotBlank;

public record SkillImportSourceRequest(@NotBlank String source) {
}
