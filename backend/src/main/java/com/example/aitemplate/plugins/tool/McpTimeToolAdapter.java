package com.example.aitemplate.plugins.tool;

import com.example.aitemplate.core.tool.ToolAdapter;
import com.example.aitemplate.core.tool.ToolCommand;
import com.example.aitemplate.core.tool.ToolResult;
import com.example.aitemplate.core.tool.ToolRiskLevel;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "app.features.mcp-enabled", havingValue = "true")
public class McpTimeToolAdapter implements ToolAdapter {

    @Override
    public String toolName() {
        return "mcp.time.now";
    }

    @Override
    public ToolRiskLevel riskLevel() {
        return ToolRiskLevel.READ;
    }

    @Override
    public ToolResult invoke(ToolCommand command) {
        return new ToolResult("Current UTC time from MCP mock tool: " + Instant.now());
    }
}
