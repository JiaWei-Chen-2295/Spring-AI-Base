package com.example.aitemplate.plugins.skill;

import com.example.aitemplate.core.skill.SkillProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "app.features.mcp-enabled", havingValue = "true")
public class DefaultMcpSkillProvider implements SkillProvider {

    @Override
    public String skillName() {
        return "mcp/default/tool-routing";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public String content() {
        return "Prefer calling tools for factual and external data. Use tool outputs as ground truth and cite tool name briefly.";
    }

    @Override
    public String augmentUserPrompt(String prompt) {
        return "[MCP Skill Active]\n" +
                "When available tools can answer the question, call them before answering.\n\n" +
                prompt;
    }
}
