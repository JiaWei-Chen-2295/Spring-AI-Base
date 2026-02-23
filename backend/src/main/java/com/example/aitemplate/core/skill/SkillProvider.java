package com.example.aitemplate.core.skill;

public interface SkillProvider {
    String skillName();

    String version();

    String content();

    default String augmentUserPrompt(String prompt) {
        return prompt;
    }

    default String postProcessAnswer(String answer) {
        return answer;
    }
}
