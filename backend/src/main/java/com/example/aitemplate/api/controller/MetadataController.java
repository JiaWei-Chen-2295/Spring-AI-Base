package com.example.aitemplate.api.controller;

import com.example.aitemplate.api.dto.ModelInfo;
import com.example.aitemplate.api.dto.SkillInfo;
import com.example.aitemplate.api.dto.ToolInfo;
import com.example.aitemplate.app.ModelRegistry;
import com.example.aitemplate.app.SkillRegistry;
import com.example.aitemplate.app.ToolRegistry;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MetadataController {

    private final ModelRegistry modelRegistry;
    private final ToolRegistry toolRegistry;
    private final SkillRegistry skillRegistry;

    public MetadataController(ModelRegistry modelRegistry, ToolRegistry toolRegistry, SkillRegistry skillRegistry) {
        this.modelRegistry = modelRegistry;
        this.toolRegistry = toolRegistry;
        this.skillRegistry = skillRegistry;
    }

    @GetMapping("/models")
    public List<ModelInfo> models() {
        return modelRegistry.list().stream()
                .map(model -> new ModelInfo(model.provider(), model.modelId(), model.capabilities(), model.health()))
                .toList();
    }

    @GetMapping("/tools")
    public List<ToolInfo> tools() {
        return toolRegistry.list().stream().map(tool -> new ToolInfo(tool.toolName(), tool.riskLevel())).toList();
    }

    @GetMapping("/skills")
    public List<SkillInfo> skills() {
        return skillRegistry.listEntries().stream()
                .map(entry -> new SkillInfo(
                        entry.provider().skillName(),
                        entry.provider().version(),
                        entry.source(),
                        entry.editable()))
                .toList();
    }
}
