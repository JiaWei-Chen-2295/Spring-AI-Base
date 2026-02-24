package com.example.aitemplate.plugins.model;

import com.example.aitemplate.core.chat.ChatCommand;
import com.example.aitemplate.core.chat.ChatResult;
import com.example.aitemplate.core.model.CapabilitySet;
import com.example.aitemplate.core.model.HealthStatus;
import com.example.aitemplate.core.model.ModelAdapter;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@ConditionalOnBean(name = "openAiChatModel")
public class OpenAiModelAdapter implements ModelAdapter {

    private final OpenAiChatModel chatModel;
    private final String modelId;
    private final String configuredApiKey;

    public OpenAiModelAdapter(
            @Qualifier("openAiChatModel") OpenAiChatModel chatModel,
            @Value("${app.models.openai-id:openai-gpt-4o}") String modelId,
            @Value("${spring.ai.openai.api-key:}") String configuredApiKey) {
        this.chatModel = chatModel;
        this.modelId = modelId;
        this.configuredApiKey = configuredApiKey;
    }

    @Override
    public String provider() {
        return "openai";
    }

    @Override
    public String modelId() {
        return modelId;
    }

    @Override
    public CapabilitySet capabilities() {
        return new CapabilitySet(true, true, true, false);
    }

    @Override
    public HealthStatus health() {
        if (configuredApiKey == null || configuredApiKey.isBlank()
                || configuredApiKey.equals("placeholder-key")) {
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
}
