package com.example.aitemplate.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.aitemplate.core.chat.ChatCommand;
import com.example.aitemplate.core.chat.ChatResult;
import com.example.aitemplate.core.model.CapabilitySet;
import com.example.aitemplate.core.model.HealthStatus;
import com.example.aitemplate.core.model.ModelAdapter;
import com.example.aitemplate.core.skill.SkillProvider;
import com.example.aitemplate.core.tool.ToolAdapter;
import com.example.aitemplate.core.tool.ToolCommand;
import com.example.aitemplate.core.tool.ToolResult;
import com.example.aitemplate.core.tool.ToolRiskLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import reactor.core.publisher.Flux;

class ChatServiceTest {

    @Test
    void shouldFallbackToModelAdapterWhenAgentModelIsUnavailable() {
        ModelAdapter model = Mockito.mock(ModelAdapter.class);
        ToolAdapter tool = Mockito.mock(ToolAdapter.class);
        SkillProvider skill = new SkillProvider() {
            @Override
            public String skillName() {
                return "team/default/summarize";
            }

            @Override
            public String version() {
                return "1.0.0";
            }

            @Override
            public String content() {
                return "Return key points first.";
            }
        };

        when(model.modelId()).thenReturn("test-model");
        when(model.provider()).thenReturn("local");
        when(model.capabilities()).thenReturn(new CapabilitySet(true, true, true, false));
        when(model.health()).thenReturn(HealthStatus.UP);
        when(model.invoke(any()))
                .thenReturn(new ChatResult("{\"tool\":\"weather.query\",\"input\":\"Beijing\"}"))
                .thenReturn(new ChatResult("Beijing weather is sunny, 26C."));
        when(tool.toolName()).thenReturn("weather.query");
        when(tool.riskLevel()).thenReturn(ToolRiskLevel.READ);
        when(tool.invoke(any(ToolCommand.class))).thenReturn(new ToolResult("Mock weather for Beijing: sunny, 26C"));

        ChatService chatService = new ChatService(
                new ModelRegistry(List.of(model)),
                new ToolRegistry(List.of(tool)),
                new SkillRegistry(List.of(skill), "target/test-skills-1", new ObjectMapper()),
                nullChatModelProvider());

        ChatCommand command = new ChatCommand(
                "c1",
                "test-model",
                "What's weather in Beijing?",
                List.of("weather.query"),
                List.of("team/default/summarize@1.0.0"));

        ChatResult result = chatService.chat(command);
        assertEquals("{\"tool\":\"weather.query\",\"input\":\"Beijing\"}", result.content());
        verify(model, times(1)).invoke(any(ChatCommand.class));
        verify(tool, times(0)).invoke(any(ToolCommand.class));
    }

    @Test
    void streamFallbackUsesModelAdapterStreamWhenAgentModelIsUnavailable() {
        ModelAdapter model = new ModelAdapter() {
            @Override
            public String provider() {
                return "local";
            }

            @Override
            public String modelId() {
                return "test-model";
            }

            @Override
            public CapabilitySet capabilities() {
                return CapabilitySet.chatOnly();
            }

            @Override
            public HealthStatus health() {
                return HealthStatus.UP;
            }

            @Override
            public ChatResult invoke(ChatCommand cmd) {
                if (cmd.message().contains("[Tool Result]")) {
                    return new ChatResult("Final answer");
                }
                return new ChatResult("{\"tool\":\"weather.query\",\"input\":\"Beijing\"}");
            }

            @Override
            public Flux<String> stream(ChatCommand cmd) {
                return Flux.just("never used");
            }
        };

        ToolAdapter tool = new ToolAdapter() {
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
                return new ToolResult("Mock weather");
            }
        };

        ChatService chatService = new ChatService(
                new ModelRegistry(List.of(model)),
                new ToolRegistry(List.of(tool)),
                new SkillRegistry(List.of(), "target/test-skills-2", new ObjectMapper()),
                nullChatModelProvider());

        List<String> chunks = chatService.stream(new ChatCommand(
                "c1",
                "test-model",
                "test",
                List.of("weather.query"),
                List.of()
        )).collectList().block();

        assertEquals(1, chunks.size());
        assertTrue(chunks.get(0).contains("never used"));
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<ChatModel> nullChatModelProvider() {
        ObjectProvider<ChatModel> provider = Mockito.mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return provider;
    }
}
