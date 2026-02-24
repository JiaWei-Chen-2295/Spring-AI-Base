package com.example.aitemplate.plugins.model;

import com.example.aitemplate.core.chat.ChatCommand;
import com.example.aitemplate.core.chat.ChatResult;
import com.example.aitemplate.core.model.CapabilitySet;
import com.example.aitemplate.core.model.HealthStatus;
import com.example.aitemplate.core.model.ModelAdapter;
import com.example.aitemplate.core.model.ModelConfig;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import reactor.core.publisher.Flux;

public class DynamicModelAdapter implements ModelAdapter {

    private final ModelConfig config;
    private final OpenAiChatModel chatModel;

    public DynamicModelAdapter(ModelConfig config) {
        this.config = config;
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(config.baseUrl())
                .apiKey(config.apiKey())
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(config.modelName())
                .build();
        this.chatModel = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .build();
    }

    @Override
    public String provider() {
        return config.provider();
    }

    @Override
    public String modelId() {
        return config.modelId();
    }

    @Override
    public CapabilitySet capabilities() {
        return config.capabilities();
    }

    @Override
    public HealthStatus health() {
        if (config.apiKey() == null || config.apiKey().isBlank()) {
            return HealthStatus.DOWN;
        }
        if (config.baseUrl() == null || config.baseUrl().isBlank()) {
            return HealthStatus.DOWN;
        }
        return HealthStatus.UP;
    }

    @Override
    public ChatResult invoke(ChatCommand command) {
        Prompt prompt = new Prompt(command.message());
        String text = chatModel.call(prompt).getResult().getOutput().getText();
        return new ChatResult(text == null ? "" : text);
    }

    @Override
    public Flux<String> stream(ChatCommand command) {
        Prompt prompt = new Prompt(command.message());
        return chatModel.stream(prompt)
                .map(response -> response.getResult().getOutput().getText())
                .filter(text -> text != null && !text.isEmpty());
    }

    public ModelConfig config() {
        return config;
    }
}
