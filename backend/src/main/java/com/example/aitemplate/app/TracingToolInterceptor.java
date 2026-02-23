package com.example.aitemplate.app;

import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import com.example.aitemplate.core.chat.ToolCallInfo;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Intercepts ALL tool calls (including shell_exec, read_skill, and custom tools)
 * to capture execution traces for frontend visibility and structured logging.
 */
class TracingToolInterceptor extends ToolInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TracingToolInterceptor.class);

    private final List<ToolCallInfo> traces;

    TracingToolInterceptor(List<ToolCallInfo> traces) {
        this.traces = traces;
    }

    static List<ToolCallInfo> newTraceList() {
        return new CopyOnWriteArrayList<>();
    }

    @Override
    public String getName() {
        return "tracing-tool-interceptor";
    }

    @Override
    public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
        String toolName = request.getToolName();
        String input = request.getArguments();
        log.info("[ToolCall] START  tool={}, input={}", toolName, truncate(input, 200));
        long start = System.currentTimeMillis();
        try {
            ToolCallResponse response = handler.call(request);
            long duration = System.currentTimeMillis() - start;
            String output = response.getResult();
            if (response.isError()) {
                log.warn("[ToolCall] ERROR  tool={}, duration={}ms, error={}", toolName, duration, truncate(output, 300));
                traces.add(new ToolCallInfo(toolName, input, "ERROR: " + output, duration));
            } else {
                log.info("[ToolCall] FINISH tool={}, duration={}ms, output={}", toolName, duration, truncate(output, 300));
                traces.add(new ToolCallInfo(toolName, input, output, duration));
            }
            return response;
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - start;
            log.error("[ToolCall] EXCEPTION tool={}, duration={}ms, error={}", toolName, duration, ex.getMessage());
            traces.add(new ToolCallInfo(toolName, input, "EXCEPTION: " + ex.getMessage(), duration));
            throw ex;
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
