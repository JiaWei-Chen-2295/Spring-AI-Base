package com.example.aitemplate.api.controller;

import com.example.aitemplate.api.dto.ConversationInfo;
import com.example.aitemplate.api.dto.MessageInfo;
import java.util.List;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ChatMemoryRepository chatMemoryRepository;
    private final ChatMemory chatMemory;

    public ConversationController(ChatMemoryRepository chatMemoryRepository, ChatMemory chatMemory) {
        this.chatMemoryRepository = chatMemoryRepository;
        this.chatMemory = chatMemory;
    }

    @GetMapping
    public List<ConversationInfo> listConversations() {
        return chatMemoryRepository.findConversationIds().stream()
                .map(ConversationInfo::new)
                .toList();
    }

    @GetMapping("/{conversationId}/messages")
    public List<MessageInfo> getMessages(@PathVariable String conversationId) {
        List<Message> messages = chatMemoryRepository.findByConversationId(conversationId);
        return messages.stream()
                .map(msg -> new MessageInfo(
                        msg.getMessageType().name().toLowerCase(),
                        msg.getText()))
                .toList();
    }

    @DeleteMapping("/{conversationId}")
    public void clearConversation(@PathVariable String conversationId) {
        chatMemory.clear(conversationId);
    }
}
