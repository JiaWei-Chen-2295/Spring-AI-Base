package com.example.aitemplate.core.model;

import com.example.aitemplate.core.chat.ChatCommand;
import com.example.aitemplate.core.chat.ChatResult;
import reactor.core.publisher.Flux;

public interface ModelAdapter {
    String provider();

    String modelId();

    CapabilitySet capabilities();

    HealthStatus health();

    ChatResult invoke(ChatCommand command);

    Flux<String> stream(ChatCommand command);
}
