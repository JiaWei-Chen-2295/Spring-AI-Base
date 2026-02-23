package com.example.aitemplate.api.dto;

import com.example.aitemplate.core.chat.ToolCallInfo;
import java.util.List;

public record ChatResponse(
        String requestId,
        String conversationId,
        String modelId,
        String content,
        List<ToolCallInfo> toolCalls
) {
    public ChatResponse(String requestId, String conversationId, String modelId, String content) {
        this(requestId, conversationId, modelId, content, List.of());
    }
}
