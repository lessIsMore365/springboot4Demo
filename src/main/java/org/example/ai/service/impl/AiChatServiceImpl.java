package org.example.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ai.function.AiFunction;
import org.example.ai.function.AiFunctionRegistry;
import org.example.ai.model.ChatMessage;
import org.example.ai.model.ChatRequest;
import org.example.ai.model.ChatResponse;
import org.example.ai.provider.AiModelProvider;
import org.example.ai.provider.AiModelRouter;
import org.example.ai.service.AiChatHistoryService;
import org.example.ai.service.AiChatService;
import org.example.ai.service.AiUsageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class AiChatServiceImpl implements AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatServiceImpl.class);

    private final AiModelRouter router;
    private final ObjectMapper objectMapper;
    private final AiFunctionRegistry functionRegistry;
    private final AiUsageService usageService;
    private final AiChatHistoryService historyService;

    public AiChatServiceImpl(
            AiModelRouter router,
            ObjectMapper objectMapper,
            AiFunctionRegistry functionRegistry,
            AiUsageService usageService,
            AiChatHistoryService historyService) {
        this.router = router;
        this.objectMapper = objectMapper;
        this.functionRegistry = functionRegistry;
        this.usageService = usageService;
        this.historyService = historyService;
    }

    @Override
    public SseEmitter chatStream(List<ChatMessage> messages) {
        return chatStreamWithFunctions(messages, false, null, null);
    }

    @Override
    public SseEmitter chatStreamWithFunctions(List<ChatMessage> messages, boolean enableFunctions) {
        return chatStreamWithFunctions(messages, enableFunctions, null, null);
    }

    @Override
    public SseEmitter chatStreamWithFunctions(List<ChatMessage> messages, boolean enableFunctions,
                                               String providerName, String sessionId) {
        AiModelProvider provider = router.resolve(providerName);
        String sid = (sessionId != null && !sessionId.isBlank()) ? sessionId : UUID.randomUUID().toString();

        SseEmitter emitter = new SseEmitter(300_000L);
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        executor.execute(() -> {
            long startTime = System.currentTimeMillis();
            boolean success = true;
            String errorMsg = null;
            List<ChatMessage> savedMessages = new ArrayList<>(messages);

            try {
                doChatLoop(savedMessages, enableFunctions, emitter, provider);
                emitter.send(SseEmitter.event().name("session_id").data(Map.of("sessionId", sid)));
                emitter.complete();
            } catch (Exception e) {
                log.error("AI chat error", e);
                success = false;
                errorMsg = e.getMessage();
                try {
                    emitter.send(SseEmitter.event().name("error").data(Map.of("error", e.getMessage())));
                } catch (IOException ignored) {}
                emitter.completeWithError(e);
            } finally {
                int latencyMs = (int) (System.currentTimeMillis() - startTime);
                try {
                    historyService.saveMessages(sid, savedMessages, provider.getModel());
                    usageService.record(provider.getModel(), "/api/ai/chat", 0,
                            0, latencyMs, null, null, success, errorMsg,
                            provider.getCostPerMillionTokens());
                } catch (Exception recErr) {
                    log.warn("Failed to record chat: {}", recErr.getMessage());
                }
            }
        });
        return emitter;
    }

    private void doChatLoop(List<ChatMessage> messages, boolean enableFunctions, SseEmitter emitter,
                            AiModelProvider provider) throws IOException {
        int maxLoops = 5;
        int maxTokens = provider.getMaxTokens();
        double temperature = provider.getTemperature();
        for (int loop = 0; loop < maxLoops; loop++) {
            ChatRequest request = buildRequest(messages, enableFunctions, provider.getModel(), maxTokens, temperature);
            String accumulated = callLLMStream(request, emitter, provider);
            boolean hasToolCalls = processToolCalls(accumulated, messages, emitter);
            if (!hasToolCalls) {
                break;
            }
        }
    }

    private ChatRequest buildRequest(List<ChatMessage> messages, boolean enableFunctions, String model,
                                      int maxTokens, double temperature) {
        List<ChatRequest.Tool> tools = enableFunctions ? functionRegistry.getToolDefinitions() : null;
        String toolChoice = enableFunctions ? "auto" : null;
        return new ChatRequest(model, messages, true, maxTokens, temperature, tools, toolChoice);
    }

    private String callLLMStream(ChatRequest request, SseEmitter emitter, AiModelProvider provider) throws IOException {
        StringBuilder contentBuf = new StringBuilder();
        Map<Integer, String> tcIds = new HashMap<>();
        Map<Integer, String> tcNames = new HashMap<>();
        Map<Integer, StringBuilder> tcArgs = new LinkedHashMap<>();

        provider.getRestClient().post()
                .uri("/v1/chat/completions")
                .body(request)
                .exchange((req, resp) -> {
                    if (resp.getStatusCode().value() != 200) {
                        String body = new String(resp.getBody().readAllBytes());
                        emitter.send(SseEmitter.event().name("error").data(Map.of("error", body)));
                        return null;
                    }
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(resp.getBody()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("data: ")) {
                                String data = line.substring(6);
                                if ("[DONE]".equals(data)) break;
                                try {
                                    ChatResponse chunk = objectMapper.readValue(data, ChatResponse.class);
                                    if (chunk.choices() != null) {
                                        for (ChatResponse.Choice choice : chunk.choices()) {
                                            if (choice.delta() != null) {
                                                if (choice.delta().content() != null) {
                                                    contentBuf.append(choice.delta().content());
                                                    emitter.send(SseEmitter.event().name("delta").data(choice.delta().content()));
                                                }
                                                if (choice.delta().toolCalls() != null) {
                                                    for (ChatResponse.ToolCallDelta tc : choice.delta().toolCalls()) {
                                                        int ti = tc.index() != null ? tc.index() : 0;
                                                        if (tc.id() != null) tcIds.put(ti, tc.id());
                                                        if (tc.function() != null) {
                                                            if (tc.function().name() != null) tcNames.put(ti, tc.function().name());
                                                            if (tc.function().arguments() != null) {
                                                                tcArgs.computeIfAbsent(ti, k -> new StringBuilder())
                                                                        .append(tc.function().arguments());
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    log.debug("Parse chunk error: {}", e.getMessage());
                                }
                            }
                        }
                    }
                    return null;
                });

        String toolCallBuf = "";
        if (!tcArgs.isEmpty()) {
            List<Map<String, String>> tcList = new ArrayList<>();
            for (Map.Entry<Integer, StringBuilder> entry : tcArgs.entrySet()) {
                int idx = entry.getKey();
                Map<String, String> tc = new LinkedHashMap<>();
                tc.put("id", tcIds.getOrDefault(idx, "call_" + idx));
                tc.put("name", tcNames.getOrDefault(idx, "unknown"));
                tc.put("arguments", entry.getValue().toString());
                tcList.add(tc);
            }
            try {
                toolCallBuf = objectMapper.writeValueAsString(tcList);
            } catch (Exception e) {
                log.warn("Failed to serialize tool calls: {}", e.getMessage());
            }
        }
        return contentBuf + (toolCallBuf.isEmpty() ? "" : "\n<<TOOL_CALLS>>" + toolCallBuf);
    }

    @SuppressWarnings("unchecked")
    private boolean processToolCalls(String accumulated, List<ChatMessage> messages, SseEmitter emitter) throws IOException {
        int idx = accumulated.indexOf("<<TOOL_CALLS>>");
        if (idx < 0) return false;

        String content = idx > 0 ? accumulated.substring(0, idx) : "";
        String raw = accumulated.substring(idx + 14);

        List<Map<String, Object>> toolCalls;
        try {
            toolCalls = objectMapper.readValue(raw, List.class);
        } catch (Exception e) {
            log.warn("Failed to parse tool calls JSON: {}", e.getMessage());
            return false;
        }

        if (toolCalls.isEmpty()) return false;

        List<ChatMessage.ToolCall> assistantToolCalls = new ArrayList<>();
        for (int i = 0; i < toolCalls.size(); i++) {
            Map<String, Object> tc = toolCalls.get(i);
            String tcId = (String) tc.get("id");
            String name = (String) tc.get("name");
            String argsStr = (String) tc.get("arguments");
            if (name == null) continue;

            ChatMessage.ToolCall.Function fn = new ChatMessage.ToolCall.Function(name, argsStr);
            ChatMessage.ToolCall call = new ChatMessage.ToolCall(tcId != null ? tcId : "call_" + i, "function", fn);
            assistantToolCalls.add(call);
        }

        // Add assistant message with tool_calls first (correct API message order)
        if (!content.isEmpty()) {
            messages.add(new ChatMessage("assistant", content, assistantToolCalls, null));
        } else {
            messages.add(ChatMessage.assistant(assistantToolCalls));
        }

        // Then add tool result messages
        for (int i = 0; i < toolCalls.size(); i++) {
            Map<String, Object> tc = toolCalls.get(i);
            String tcId = (String) tc.get("id");
            String name = (String) tc.get("name");
            String argsStr = (String) tc.get("arguments");
            if (name == null) continue;

            Map<String, Object> args = Map.of();
            if (argsStr != null && !argsStr.isEmpty()) {
                try {
                    args = objectMapper.readValue(argsStr, Map.class);
                } catch (Exception e) {
                    log.debug("Parse args error: {}", e.getMessage());
                }
            }

            AiFunction aiFn = functionRegistry.get(name);
            String toolResult;
            try {
                Object result = aiFn != null ? aiFn.execute(args) : Map.of("error", "未知函数: " + name);
                toolResult = objectMapper.writeValueAsString(result);
            } catch (Exception e) {
                toolResult = "{\"error\": \"" + e.getMessage() + "\"}";
            }

            messages.add(ChatMessage.tool(tcId != null ? tcId : "call_" + i, toolResult));
            emitter.send(SseEmitter.event().name("tool_call").data(Map.of("function", name, "result", toolResult)));
        }

        return true;
    }
}
