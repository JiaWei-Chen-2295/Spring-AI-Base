package com.example.aitemplate.api.dto;

import java.util.List;

public record SkillImportShResponse(int imported, List<String> errors) {
}
