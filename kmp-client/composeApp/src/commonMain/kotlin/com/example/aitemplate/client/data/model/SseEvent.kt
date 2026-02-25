package com.example.aitemplate.client.data.model

/**
 * Represents a parsed Server-Sent Event from the chat stream.
 */
sealed class SseEvent {
    data class Token(val text: String) : SseEvent()
    data class ToolCall(val info: ToolCallInfo) : SseEvent()
    /** Live progress update for an in-flight tool call (identified by toolName + partial output). */
    data class ToolCallProgress(val info: ToolCallInfo) : SseEvent()
    data class SkillApply(val skills: List<SkillApplyInfo>) : SseEvent()
    data class Error(val message: String) : SseEvent()
    data object Done : SseEvent()
}
