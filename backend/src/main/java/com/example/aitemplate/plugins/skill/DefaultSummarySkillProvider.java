package com.example.aitemplate.plugins.skill;

import com.example.aitemplate.core.skill.SkillProvider;
import org.springframework.stereotype.Component;

@Component
public class DefaultSummarySkillProvider implements SkillProvider {

    @Override
    public String skillName() {
        return "team/default/summarize";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public String content() {
        return "You are a concise assistant. Return key points first, then details.";
    }

    @Override
    public String postProcessAnswer(String answer) {
        if (answer == null || answer.isBlank()) {
            return answer;
        }
        if (answer.startsWith("- ") || answer.startsWith("1. ")) {
            return answer;
        }
        return "- Key point: " + answer.trim();
    }
}
