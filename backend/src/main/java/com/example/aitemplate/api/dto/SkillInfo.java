package com.example.aitemplate.api.dto;

public record SkillInfo(String skillName, String version, String source, boolean editable) {

    public SkillInfo(String skillName, String version) {
        this(skillName, version, "builtin", false);
    }
}
