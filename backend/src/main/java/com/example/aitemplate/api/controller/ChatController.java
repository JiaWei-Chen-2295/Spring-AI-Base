package com.example.aitemplate.api.controller;

import com.example.aitemplate.api.dto.ChatRequest;
import com.example.aitemplate.api.dto.ChatResponse;
import com.example.aitemplate.app.ChatService;
import com.example.aitemplate.core.chat.ChatCommand;
import com.example.aitemplate.core.chat.ChatResult;
import com.example.aitemplate.infra.http.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
@Validated
public class ChatController {

    private record TokenPayload(String token) {
    }

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request, HttpServletRequest servletRequest) {
        ChatCommand command = new ChatCommand(
                request.conversationId(),
                request.modelId(),
                request.message(),
                request.tools(),
                request.skills()
        );
        String requestId = servletRequest.getHeader(RequestIdFilter.REQUEST_ID_HEADER);
        ChatResult result = chatService.chat(command);
        return new ChatResponse(
                requestId,
                request.conversationId(),
                request.modelId(),
                result.content(),
                result.toolCalls()
        );
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> stream(
            @RequestParam String conversationId,
            @RequestParam String model,
            @RequestParam String message,
            @RequestParam(required = false) List<String> tools,
            @RequestParam(required = false) List<String> skills
    ) {
        ChatCommand command = new ChatCommand(conversationId, model, message, tools, skills);

        Flux<ServerSentEvent<Object>> events = chatService.streamWithToolTrace(command)
                .map(item -> {
                    if (item.startsWith("TOOL_CALL:")) {
                        String json = item.substring("TOOL_CALL:".length());
                        return ServerSentEvent.builder((Object) json).event("tool_call").build();
                    }
                    if (item.startsWith("TOOL_CALL_PROGRESS:")) {
                        String json = item.substring("TOOL_CALL_PROGRESS:".length());
                        return ServerSentEvent.builder((Object) json).event("tool_call_progress").build();
                    }
                    if (item.startsWith("SKILL_APPLY:")) {
                        String json = item.substring("SKILL_APPLY:".length());
                        return ServerSentEvent.builder((Object) json).event("skill_apply").build();
                    }
                    return ServerSentEvent.builder((Object) new TokenPayload(item)).event("token").build();
                });

        Flux<ServerSentEvent<Object>> safeEvents = events.onErrorResume(ex -> Flux.just(
                ServerSentEvent.builder((Object) (ex.getMessage() == null ? "stream error" : ex.getMessage()))
                        .event("error")
                        .build()
        ));

        Flux<ServerSentEvent<Object>> done = Flux.just(ServerSentEvent.builder((Object) "done").event("done").build());
        return safeEvents.concatWith(done);
    }
}
