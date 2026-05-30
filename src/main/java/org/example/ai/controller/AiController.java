package org.example.ai.controller;

import org.example.ai.function.AiFunctionRegistry;
import org.example.ai.model.ChatMessage;
import org.example.ai.provider.AiModelRouter;
import org.example.ai.service.AiChatService;
import org.example.ai.service.AiInspectService;
import org.example.ai.service.Chat2SqlService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiChatService aiChatService;
    private final AiFunctionRegistry functionRegistry;
    private final AiInspectService inspectService;
    private final Chat2SqlService chat2SqlService;
    private final AiModelRouter modelRouter;

    public AiController(AiChatService aiChatService, AiFunctionRegistry functionRegistry,
                        AiInspectService inspectService, Chat2SqlService chat2SqlService,
                        AiModelRouter modelRouter) {
        this.aiChatService = aiChatService;
        this.functionRegistry = functionRegistry;
        this.inspectService = inspectService;
        this.chat2SqlService = chat2SqlService;
        this.modelRouter = modelRouter;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody AiChatBody body) {
        List<ChatMessage> messages = body.messages() != null ? body.messages() : List.of();
        boolean enableFunctions = body.enableFunctions() != null && body.enableFunctions();
        String provider = body.provider();
        String sessionId = body.sessionId();
        if (enableFunctions && !functionRegistry.getAll().isEmpty()) {
            return aiChatService.chatStreamWithFunctions(messages, true, provider, sessionId);
        }
        return aiChatService.chatStreamWithFunctions(messages, false, provider, sessionId);
    }

    @GetMapping("/functions")
    public Map<String, Object> getFunctions() {
        return functionRegistry.getFunctionList();
    }

    @GetMapping("/inspect")
    public Map<String, Object> inspect(@RequestParam(defaultValue = "all") String target,
                                       @RequestParam(defaultValue = "deepseek") String provider) {
        return inspectService.inspect(target, provider);
    }

    @PostMapping("/chat2sql")
    public Map<String, Object> chat2sql(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        String provider = body.getOrDefault("provider", "deepseek");
        if (question == null || question.isBlank()) {
            return Map.of("error", "question is required");
        }
        try {
            return chat2SqlService.query(question, provider);
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    @GetMapping("/providers")
    public Map<String, Object> getProviders() {
        return modelRouter.getAvailableProviders();
    }

    public record AiChatBody(
            List<ChatMessage> messages,
            Boolean enableFunctions,
            String provider,
            String sessionId
    ) {}
}
