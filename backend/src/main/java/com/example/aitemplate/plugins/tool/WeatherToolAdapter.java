package com.example.aitemplate.plugins.tool;

import com.example.aitemplate.core.tool.ToolAdapter;
import com.example.aitemplate.core.tool.ToolCommand;
import com.example.aitemplate.core.tool.ToolResult;
import com.example.aitemplate.core.tool.ToolRiskLevel;
import org.springframework.stereotype.Component;

@Component
public class WeatherToolAdapter implements ToolAdapter {

    @Override
    public String toolName() {
        return "weather.query";
    }

    @Override
    public ToolRiskLevel riskLevel() {
        return ToolRiskLevel.READ;
    }

    @Override
    public ToolResult invoke(ToolCommand command) {
        String location = (command.input() == null || command.input().isBlank()) ? "Shanghai" : command.input();
        return new ToolResult("Mock weather for " + location + ": sunny, 26C");
    }
}
