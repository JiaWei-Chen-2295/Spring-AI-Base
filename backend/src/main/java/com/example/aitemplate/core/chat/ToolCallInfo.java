package com.example.aitemplate.core.chat;

/**
 * Captures metadata about a single tool invocation during agent execution.
 */
public record ToolCallInfo(
        String toolName,
        String input,
        String output,
        long durationMs
) {}
