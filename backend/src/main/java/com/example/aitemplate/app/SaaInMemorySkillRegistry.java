package com.example.aitemplate.app;

import com.alibaba.cloud.ai.graph.skills.SkillMetadata;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;
import com.example.aitemplate.core.skill.SkillProvider;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;

public class SaaInMemorySkillRegistry implements SkillRegistry {

    private final Map<String, SkillProvider> providerMap;
    private final List<SkillMetadata> metadata;
    private final SystemPromptTemplate systemPromptTemplate;

    public SaaInMemorySkillRegistry(List<SkillProvider> providers) {
        this.providerMap = providers.stream()
                .collect(Collectors.toUnmodifiableMap(SkillProvider::skillName, Function.identity(), (left, right) -> right));
        this.metadata = providers.stream()
                .map(provider -> SkillMetadata.builder()
                        .name(provider.skillName())
                        .description(provider.skillName() + "@" + provider.version())
                        .skillPath("in-memory://" + provider.skillName())
                        .source("in-memory")
                        .fullContent(provider.content())
                        .build())
                .toList();
        this.systemPromptTemplate = new SystemPromptTemplate(
                "Skill system is enabled. Use read_skill(skill_name) when you need skill instructions.");
    }

    @Override
    public Optional<SkillMetadata> get(String name) {
        return metadata.stream().filter(item -> item.getName().equals(name)).findFirst();
    }

    @Override
    public List<SkillMetadata> listAll() {
        return metadata;
    }

    @Override
    public boolean contains(String name) {
        return providerMap.containsKey(name);
    }

    @Override
    public int size() {
        return providerMap.size();
    }

    @Override
    public void reload() {
        // In-memory registry, no external source to reload.
    }

    @Override
    public String readSkillContent(String name) throws IOException {
        SkillProvider provider = providerMap.get(name);
        if (provider == null) {
            throw new IOException("Unknown skill: " + name);
        }
        return provider.content();
    }

    @Override
    public String getSkillLoadInstructions() {
        return "In-memory skill registry loaded from application SkillProvider beans.";
    }

    @Override
    public String getRegistryType() {
        return "InMemory";
    }

    @Override
    public SystemPromptTemplate getSystemPromptTemplate() {
        return systemPromptTemplate;
    }
}
