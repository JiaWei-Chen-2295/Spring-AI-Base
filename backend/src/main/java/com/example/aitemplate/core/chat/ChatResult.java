package com.example.aitemplate.core.chat;

import java.util.List;

public record ChatResult(String content, List<ToolCallInfo> toolCalls) {

    public ChatResult(String content) {
        this(content, List.of());
    }
}
