package org.example.ai.service;

import org.example.ai.model.ChatMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface AiChatService {

    SseEmitter chatStream(List<ChatMessage> messages);

    SseEmitter chatStreamWithFunctions(List<ChatMessage> messages, boolean enableFunctions);

    SseEmitter chatStreamWithFunctions(List<ChatMessage> messages, boolean enableFunctions, String providerName, String sessionId);
}
