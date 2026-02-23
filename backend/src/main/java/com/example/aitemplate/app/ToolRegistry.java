package com.example.aitemplate.app;

import com.example.aitemplate.core.tool.ToolAdapter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ToolRegistry {

    private final List<ToolAdapter> tools;
    private final Map<String, ToolAdapter> toolMap;

    public ToolRegistry(List<ToolAdapter> tools) {
        this.tools = List.copyOf(tools);
        this.toolMap = this.tools.stream().collect(Collectors.toUnmodifiableMap(ToolAdapter::toolName, Function.identity()));
    }

    public List<ToolAdapter> list() {
        return tools;
    }

    public List<ToolAdapter> resolve(List<String> toolNames) {
        if (toolNames == null || toolNames.isEmpty()) {
            return List.of();
        }
        return toolNames.stream()
                .map(toolMap::get)
                .filter(tool -> tool != null)
                .toList();
    }
}
