package com.example.aitemplate.plugins.model;

import com.example.aitemplate.core.chat.ChatCommand;
import com.example.aitemplate.core.chat.ChatResult;
import com.example.aitemplate.core.model.CapabilitySet;
import com.example.aitemplate.core.model.HealthStatus;
import com.example.aitemplate.core.model.ModelAdapter;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class EchoModelAdapter implements ModelAdapter {

    @Override
    public String provider() {
        return "local";
    }

    @Override
    public String modelId() {
        return "local-echo";
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
    public ChatResult invoke(ChatCommand command) {
        return new ChatResult("echo: " + command.message());
    }

    @Override
    public Flux<String> stream(ChatCommand command) {
        String[] tokens = ("echo: " + command.message()).split(" ");
        return Flux.fromArray(tokens).map(token -> token + " ");
    }
}
