package com.example.aitemplate.core.tool;

public interface ToolAdapter {
    String toolName();

    ToolRiskLevel riskLevel();

    ToolResult invoke(ToolCommand command);
}
