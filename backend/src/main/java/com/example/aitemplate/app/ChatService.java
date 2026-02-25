package com.example.aitemplate.app;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.Builder;
import com.alibaba.cloud.ai.graph.agent.hook.Hook;
import com.alibaba.cloud.ai.graph.agent.hook.shelltool.ShellToolAgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;
import com.alibaba.cloud.ai.graph.agent.tools.ShellTool2;
import com.google.gson.Gson;
import com.example.aitemplate.core.chat.ChatCommand;
import com.example.aitemplate.core.chat.ChatResult;
import com.example.aitemplate.core.chat.ToolCallInfo;
import com.example.aitemplate.core.model.ModelAdapter;
import com.example.aitemplate.core.skill.SkillProvider;
import com.example.aitemplate.core.tool.ToolAdapter;
import com.example.aitemplate.core.tool.ToolCommand;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final Gson GSON = new Gson();

    private final ModelRegistry modelRegistry;
    private final ToolRegistry toolRegistry;
    private final SkillRegistry skillRegistry;
    private final ChatMemory chatMemory;
    private final ObjectProvider<ChatModel> springChatModelProvider;
    private final String openAiBaseUrl;

    public ChatService(
            ModelRegistry modelRegistry,
            ToolRegistry toolRegistry,
            SkillRegistry skillRegistry,
            ChatMemory chatMemory,
            ObjectProvider<ChatModel> springChatModelProvider,
            @org.springframework.beans.factory.annotation.Value("${spring.ai.openai.base-url:}") String openAiBaseUrl) {
        this.modelRegistry = modelRegistry;
        this.toolRegistry = toolRegistry;
        this.skillRegistry = skillRegistry;
        this.chatMemory = chatMemory;
        this.springChatModelProvider = springChatModelProvider;
        this.openAiBaseUrl = openAiBaseUrl == null ? "" : openAiBaseUrl;
    }

    public ChatResult chat(ChatCommand command) {
        List<ToolAdapter> selectedTools = toolRegistry.resolve(command.tools());
        List<SkillProvider> selectedSkills = skillRegistry.resolve(command.skills());

        // Retrieve conversation history and save current user message
        List<Message> history = chatMemory.get(command.conversationId());
        chatMemory.add(command.conversationId(), new UserMessage(command.message()));

        ChatResult result;
        ChatModel springChatModel = springChatModelProvider.getIfAvailable();
        if (springChatModel != null && isAgentEnabledModel(command.modelId())) {
            result = chatWithSaaAgent(command, springChatModel, selectedTools, selectedSkills, history);
        } else {
            result = modelRegistry.getOrThrow(command.modelId()).invoke(command);
        }

        // Save assistant response to memory
        chatMemory.add(command.conversationId(), new AssistantMessage(result.content()));
        return result;
    }

    public Flux<String> stream(ChatCommand command) {
        ChatModel springChatModel = springChatModelProvider.getIfAvailable();
        if (springChatModel != null && isAgentEnabledModel(command.modelId())) {
            return Flux.just(chat(command).content());
        }
        return modelRegistry.getOrThrow(command.modelId()).stream(command);
    }

    /**
     * Stream that also emits tool call metadata as SSE events.
     * Returns a Flux of tagged strings: "TOOL_CALL:{json}" or plain text tokens.
     */
    public Flux<String> streamWithToolTrace(ChatCommand command) {
        ChatModel springChatModel = springChatModelProvider.getIfAvailable();
        boolean hasToolsOrSkills = (command.tools() != null && !command.tools().isEmpty())
                || (command.skills() != null && !command.skills().isEmpty());

        if (springChatModel != null && isAgentEnabledModel(command.modelId()) && hasToolsOrSkills) {
            return streamWithLiveAgentEvents(command, springChatModel);
        }

        // Direct stream path: real token-by-token streaming with memory persistence
        chatMemory.add(command.conversationId(), new UserMessage(command.message()));
        StringBuilder collected = new StringBuilder();
        return modelRegistry.getOrThrow(command.modelId()).stream(command)
                .doOnNext(collected::append)
                .doOnComplete(() ->
                        chatMemory.add(command.conversationId(), new AssistantMessage(collected.toString())));
    }

    private Flux<String> streamWithLiveAgentEvents(ChatCommand command, ChatModel springChatModel) {
        List<ToolAdapter> selectedTools = toolRegistry.resolve(command.tools());
        List<SkillProvider> selectedSkills = skillRegistry.resolve(command.skills());
        List<Message> history = chatMemory.get(command.conversationId());
        chatMemory.add(command.conversationId(), new UserMessage(command.message()));

        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        Schedulers.boundedElastic().schedule(() -> {
            try {
                if (!selectedSkills.isEmpty()) {
                    sink.tryEmitNext("SKILL_APPLY:" + GSON.toJson(
                            selectedSkills.stream()
                                    .map(s -> new SkillApplyInfo(s.skillName(), s.version()))
                                    .toList()));
                }

                List<ToolCallInfo> traces = TracingToolInterceptor.newTraceList();
                AtomicInteger seq = new AtomicInteger(0);
                Map<String, Deque<String>> callIdMap = new ConcurrentHashMap<>();
                TracingToolInterceptor.ToolCallListener listener = new TracingToolInterceptor.ToolCallListener() {
                    @Override
                    public void onStart(String toolName, String input, long startedAt) {
                        String callId = "tc-" + seq.incrementAndGet();
                        callIdMap.computeIfAbsent(toolKey(toolName, input), __ -> new ConcurrentLinkedDeque<>()).addLast(callId);
                        sink.tryEmitNext("TOOL_CALL_PROGRESS:" + GSON.toJson(
                                new ToolCallProgressInfo(callId, toolName, input, "", null, "running", startedAt)));
                    }

                    @Override
                    public void onFinish(ToolCallInfo info, boolean isError) {
                        String key = toolKey(info.toolName(), info.input());
                        Deque<String> deque = callIdMap.get(key);
                        String callId = (deque == null || deque.isEmpty()) ? ("tc-" + seq.incrementAndGet()) : deque.pollFirst();
                        sink.tryEmitNext("TOOL_CALL_PROGRESS:" + GSON.toJson(
                                new ToolCallProgressInfo(
                                        callId,
                                        info.toolName(),
                                        info.input(),
                                        info.output(),
                                        info.durationMs(),
                                        isError ? "error" : "done",
                                        System.currentTimeMillis())));
                    }
                };

                String instruction = buildAgentInstruction(selectedSkills, selectedTools, history);
                Builder builder = ReactAgent.builder()
                        .name("chat-agent")
                        .model(springChatModel)
                        .instruction(instruction)
                        .tools(toToolCallbacks(selectedTools))
                        .interceptors(new TracingToolInterceptor(traces, listener));

                if (springChatModel instanceof OpenAiChatModel) {
                    String runtimeModel = resolveRuntimeModelNameForAgent(command.modelId());
                    if (!runtimeModel.isBlank()) {
                        builder.chatOptions(OpenAiChatOptions.builder().model(runtimeModel).build());
                    }
                }

                List<Hook> hooks = new ArrayList<>();
                if (!selectedSkills.isEmpty()) {
                    hooks.add(SkillsAgentHook.builder()
                            .skillRegistry(new SaaInMemorySkillRegistry(selectedSkills))
                            .autoReload(false)
                            .build());
                }
                ShellToolAgentHook shellToolHook = buildShellToolHookIfNeeded(selectedSkills);
                if (shellToolHook != null) {
                    hooks.add(shellToolHook);
                }
                if (!hooks.isEmpty()) {
                    builder.hooks(hooks);
                }

                ReactAgent agent = builder.build();
                log.info("[Agent] Dispatching message to agent (live stream). model={}, tools={}, skills={}",
                        command.modelId(),
                        selectedTools.stream().map(ToolAdapter::toolName).toList(),
                        selectedSkills.stream().map(SkillProvider::skillName).toList());

                AssistantMessage result = agent.call(command.message());
                String text = result == null ? "" : result.getText();
                chatMemory.add(command.conversationId(), new AssistantMessage(text));
                sink.tryEmitNext(text == null ? "" : text);
                sink.tryEmitComplete();
            }
            catch (Exception ex) {
                try {
                    ChatResult fallback = invokeWithModelFallback(command, ex);
                    chatMemory.add(command.conversationId(), new AssistantMessage(fallback.content()));
                    sink.tryEmitNext(fallback.content());
                    sink.tryEmitComplete();
                }
                catch (Exception finalEx) {
                    sink.tryEmitError(finalEx);
                }
            }
        });

        return sink.asFlux();
    }

    private ChatResult chatWithSaaAgent(
            ChatCommand command,
            ChatModel springChatModel,
            List<ToolAdapter> selectedTools,
            List<SkillProvider> selectedSkills,
            List<Message> history) {
        try {
            List<ToolCallInfo> traces = TracingToolInterceptor.newTraceList();

            String instruction = buildAgentInstruction(selectedSkills, selectedTools, history);
            if (!selectedSkills.isEmpty()) {
                log.info("[Skill] Applying {} skill(s): {}",
                        selectedSkills.size(),
                        selectedSkills.stream().map(s -> s.skillName() + "@" + s.version()).toList());
                log.debug("[Skill] Instruction preview: {}",
                        instruction.length() > 400 ? instruction.substring(0, 400) + "..." : instruction);
            }

            Builder builder = ReactAgent.builder()
                    .name("chat-agent")
                    .model(springChatModel)
                    .instruction(instruction)
                    .tools(toToolCallbacks(selectedTools))
                    .interceptors(new TracingToolInterceptor(traces));

            if (springChatModel instanceof OpenAiChatModel) {
                String runtimeModel = resolveRuntimeModelNameForAgent(command.modelId());
                if (!runtimeModel.isBlank()) {
                    builder.chatOptions(OpenAiChatOptions.builder().model(runtimeModel).build());
                }
            }

            List<Hook> hooks = new ArrayList<>();
            if (!selectedSkills.isEmpty()) {
                SkillsAgentHook skillHook = SkillsAgentHook.builder()
                        .skillRegistry(new SaaInMemorySkillRegistry(selectedSkills))
                        .autoReload(false)
                        .build();
                hooks.add(skillHook);
            }

            ShellToolAgentHook shellToolHook = buildShellToolHookIfNeeded(selectedSkills);
            if (shellToolHook != null) {
                hooks.add(shellToolHook);
            }
            if (!hooks.isEmpty()) {
                builder.hooks(hooks);
            }

            ReactAgent agent = builder.build();

            log.info("[Agent] Dispatching message to agent. model={}, tools={}, skills={}",
                    command.modelId(),
                    selectedTools.stream().map(ToolAdapter::toolName).toList(),
                    selectedSkills.stream().map(SkillProvider::skillName).toList());

            AssistantMessage result = agent.call(command.message());
            String text = result == null ? "" : result.getText();

            if (!traces.isEmpty()) {
                log.info("[Agent] Tool calls executed ({}): {}", traces.size(),
                        traces.stream()
                                .map(t -> t.toolName() + "(" + t.durationMs() + "ms)")
                                .toList());
            }
            log.info("[Agent] Completed. model={}, skills={}, toolCalls={}, responseLength={}",
                    command.modelId(),
                    selectedSkills.stream().map(SkillProvider::skillName).toList(),
                    traces.size(),
                    text == null ? 0 : text.length());

            return new ChatResult(text == null ? "" : text, List.copyOf(traces));
        }
        catch (Exception ex) {
            return invokeWithModelFallback(command, ex);
        }
    }

    private ChatResult invokeWithModelFallback(ChatCommand command, Exception originalEx) {
        ModelAdapter requestedAdapter = modelRegistry.getOrThrow(command.modelId());
        ModelAdapter dashScopeAdapter = modelRegistry.list().stream()
                .filter(model -> "dashscope".equalsIgnoreCase(model.provider()))
                .findFirst()
                .orElse(null);

        if (dashScopeAdapter != null && !requestedAdapter.modelId().equalsIgnoreCase(dashScopeAdapter.modelId())) {
            try {
                log.warn(
                        "Agent model call failed on modelId={}, provider={}. Fallback to DashScope modelId={}. Cause={}",
                        requestedAdapter.modelId(),
                        requestedAdapter.provider(),
                        dashScopeAdapter.modelId(),
                        originalEx.getMessage());
                return dashScopeAdapter.invoke(command);
            }
            catch (Exception dashScopeEx) {
                log.error(
                        "DashScope fallback failed on modelId={}. Fallback cause={}",
                        dashScopeAdapter.modelId(),
                        dashScopeEx.getMessage(),
                        dashScopeEx);
            }
        }
        else {
            log.warn(
                    "Agent model call failed on modelId={}, provider={}, no alternate DashScope fallback found. Cause={}",
                    requestedAdapter.modelId(),
                    requestedAdapter.provider(),
                    originalEx.getMessage());
        }

        return requestedAdapter.invoke(command);
    }

    private List<ToolCallback> toToolCallbacks(List<ToolAdapter> selectedTools) {
        return selectedTools.stream()
                .map(tool -> (ToolCallback) FunctionToolCallback.builder(
                                tool.toolName(),
                                (ToolInput input) -> tool.invoke(new ToolCommand(input == null ? "" : input.input())).output())
                        .description("Tool from adapter: " + tool.toolName() + ", risk=" + tool.riskLevel())
                        .inputType(ToolInput.class)
                        .build())
                .toList();
    }

    private String buildAgentInstruction(List<SkillProvider> selectedSkills, List<ToolAdapter> selectedTools, List<Message> history) {
        String base;
        if (selectedSkills.isEmpty()) {
            base = "You are a helpful assistant. Use available tools when needed.";
        } else {
            String skillNames = selectedSkills.stream().map(SkillProvider::skillName).toList().toString();
            String pythonGuide = buildPythonExecutionGuide(selectedSkills);
            String toolsGuide = selectedTools.isEmpty() ? "(none)" : selectedTools.stream().map(ToolAdapter::toolName).toList().toString();
            base = """
                    You are a helpful assistant.
                    Skill system is enabled. Before solving complex tasks, use read_skill(skill_name) to load needed skills.
                    Preferred skills for this request: %s
                    Available business tools: %s
                    Use tools when relevant and provide a direct final answer.

                    %s
                    """.formatted(skillNames, toolsGuide, pythonGuide);
        }

        String historyBlock = formatHistoryForAgent(history);
        if (historyBlock.isEmpty()) {
            return base;
        }
        return base + "\n\n## Conversation History\n" + historyBlock
                + "\n\nContinue the conversation naturally based on the history above.";
    }

    private String formatHistoryForAgent(List<Message> history) {
        if (history == null || history.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Message msg : history) {
            String role = switch (msg.getMessageType()) {
                case USER -> "User";
                case ASSISTANT -> "Assistant";
                case SYSTEM -> "System";
                default -> "Unknown";
            };
            sb.append(role).append(": ").append(msg.getText()).append("\n");
        }
        return sb.toString().trim();
    }

    private ShellToolAgentHook buildShellToolHookIfNeeded(List<SkillProvider> selectedSkills) {
        boolean hasPythonSkill = false;
        for (SkillProvider skill : selectedSkills) {
            Optional<Path> script = skillRegistry.findPythonSkillScript(skill.skillName(), skill.version());
            if (script.isPresent()) {
                hasPythonSkill = true;
                log.info("[Skill] Python script found for {}@{}: {}", skill.skillName(), skill.version(), script.get());
            } else {
                log.info("[Skill] No Python script found for {}@{} — shell_exec will NOT be registered", skill.skillName(), skill.version());
            }
        }
        if (!hasPythonSkill) {
            return null;
        }
        List<String> shellCommand = isWindows()
                ? List.of("powershell", "-NoLogo", "-NoProfile", "-ExecutionPolicy", "Bypass", "-NoExit")
                : List.of("bash", "-i");

        ShellTool2 shellTool2 = ShellTool2.builder(
                        "Execute local python skills only. Avoid network and destructive commands.")
                .withShellCommand(shellCommand)
                .withCommandTimeout(120_000L)
                .withMaxOutputLines(300)
                .build();
        return ShellToolAgentHook.builder()
                .shellTool2(shellTool2)
                .shellToolName("shell_exec")
                .build();
    }

    private String buildPythonExecutionGuide(List<SkillProvider> selectedSkills) {
        List<String> lines = new ArrayList<>();
        for (SkillProvider skill : selectedSkills) {
            Path script = skillRegistry.findPythonSkillScript(skill.skillName(), skill.version()).orElse(null);
            if (script == null) {
                continue;
            }
            String path = script.toString().replace("\\", "/");
            lines.add("- " + skill.skillName() + " => python \"" + path + "\"");
        }
        if (lines.isEmpty()) {
            return "No python skill script selected.";
        }
        return """
                If you need to run local python skills, you MUST call tool `shell` (do not only print commands in text).
                Use one of:
                %s
                """.formatted(String.join("\n", lines));
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private boolean isAgentEnabledModel(String modelId) {
        ModelAdapter adapter = modelRegistry.getOrThrow(modelId);
        if (adapter instanceof com.example.aitemplate.plugins.model.DynamicModelAdapter) {
            return false;
        }
        return !"local".equalsIgnoreCase(adapter.provider());
    }

    private String resolveRuntimeModelName(String modelId) {
        int split = modelId.indexOf('-');
        if (split < 0 || split + 1 >= modelId.length()) {
            return modelId;
        }
        return modelId.substring(split + 1);
    }

    private String resolveRuntimeModelNameForAgent(String modelId) {
        String runtime = resolveRuntimeModelName(modelId);
        ModelAdapter requested = modelRegistry.getOrThrow(modelId);
        if (!isDashScopeCompatibleEndpoint() || !"openai".equalsIgnoreCase(requested.provider())) {
            return runtime;
        }
        if (runtime.toLowerCase().startsWith("qwen")) {
            return runtime;
        }

        ModelAdapter dashScopeAdapter = modelRegistry.list().stream()
                .filter(model -> "dashscope".equalsIgnoreCase(model.provider()))
                .findFirst()
                .orElse(null);
        if (dashScopeAdapter == null) {
            return runtime;
        }

        String fallbackRuntime = resolveRuntimeModelName(dashScopeAdapter.modelId());
        if (fallbackRuntime.isBlank()) {
            return runtime;
        }
        log.warn(
                "Detected DashScope-compatible endpoint with non-qwen OpenAI runtime model '{}'. Auto-correct to '{}'.",
                runtime,
                fallbackRuntime);
        return fallbackRuntime;
    }

    private boolean isDashScopeCompatibleEndpoint() {
        return openAiBaseUrl.toLowerCase().contains("dashscope.aliyuncs.com/compatible-mode");
    }

    private String toolKey(String toolName, String input) {
        return (toolName == null ? "" : toolName) + "|" + (input == null ? "" : input);
    }

    private record ToolInput(String input) {
    }

    private record SkillApplyInfo(String name, String version) {
    }

    private record ToolCallProgressInfo(
            String callId,
            String toolName,
            String input,
            String output,
            Long durationMs,
            String status,
            long timestamp
    ) {
    }
}
