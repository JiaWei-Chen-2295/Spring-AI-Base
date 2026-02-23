package com.example.aitemplate.api.dto;

import jakarta.validation.constraints.NotBlank;

public record SkillImportShRequest(@NotBlank String script) {
}
