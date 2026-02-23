package com.example.aitemplate.plugins.model;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.example.aitemplate.core.chat.ChatCommand;
import com.example.aitemplate.core.chat.ChatResult;
import com.example.aitemplate.core.model.CapabilitySet;
import com.example.aitemplate.core.model.HealthStatus;
import com.example.aitemplate.core.model.ModelAdapter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
@ConditionalOnExpression("'${DASHSCOPE_API_KEY:}' != ''")
public class DashScopeModelAdapter implements ModelAdapter {

    private final String apiKey;
    private final String modelName;
    private final String modelId;

    public DashScopeModelAdapter(
            @Value("${DASHSCOPE_API_KEY:}") String apiKey,
            @Value("${app.dashscope.chat-model:qwen-plus}") String modelName,
            @Value("${app.models.dashscope-id:dashscope-qwen-plus}") String modelId) {
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.modelId = modelId;
    }

    @Override
    public String provider() {
        return "dashscope";
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
        return apiKey == null || apiKey.isBlank() ? HealthStatus.DOWN : HealthStatus.UP;
    }

    @Override
    public ChatResult invoke(ChatCommand command) {
        Generation client = new Generation();
        GenerationParam param = baseParam(command.message()).build();
        try {
            GenerationResult completion = client.call(param);
            String content = extractContent(completion);
            return new ChatResult(content == null ? "" : content);
        }
        catch (ApiException | NoApiKeyException | InputRequiredException ex) {
            throw new IllegalArgumentException("DashScope call failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Flux<String> stream(ChatCommand command) {
        Generation client = new Generation();
        GenerationParam param = baseParam(command.message()).incrementalOutput(true).build();
        try {
            return Flux.from(client.streamCall(param))
                    .map(this::extractContent)
                    .filter(text -> text != null && !text.isEmpty())
                    .onErrorResume(ex -> Flux.just(invoke(command).content()));
        }
        catch (ApiException | NoApiKeyException | InputRequiredException ex) {
            return Flux.just(invoke(command).content());
        }
    }

    private GenerationParam.GenerationParamBuilder<?, ?> baseParam(String message) {
        Message userMessage = Message.builder()
                .role(Role.USER.getValue())
                .content(message)
                .build();

        return GenerationParam.builder()
                .apiKey(apiKey)
                .model(modelName)
                .resultFormat("message")
                .messages(List.of(userMessage));
    }

    private String extractContent(GenerationResult result) {
        if (result == null || result.getOutput() == null) {
            return "";
        }

        if (result.getOutput().getChoices() != null && !result.getOutput().getChoices().isEmpty()) {
            Message message = result.getOutput().getChoices().get(0).getMessage();
            if (message != null && message.getContent() != null) {
                return message.getContent();
            }
        }

        return result.getOutput().getText() == null ? "" : result.getOutput().getText();
    }
}
