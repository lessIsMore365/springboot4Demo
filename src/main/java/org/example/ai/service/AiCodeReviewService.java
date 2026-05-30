package org.example.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ai.provider.AiModelProvider;
import org.example.ai.provider.AiModelRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AiCodeReviewService {

    private static final Logger log = LoggerFactory.getLogger(AiCodeReviewService.class);
    private final AiModelRouter router;
    private final ObjectMapper objectMapper;

    public AiCodeReviewService(AiModelRouter router, ObjectMapper objectMapper) {
        this.router = router;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> review(String code, String language, String providerName) {
        AiModelProvider provider = router.resolve(providerName);

        String systemPrompt = """
                你是一位资深Java代码评审专家。请对提供的代码进行全面评审，重点关注以下方面：
                1. 空指针风险（NullPointerException）
                2. SQL注入风险
                3. 并发安全问题（竞态条件、死锁、线程安全）
                4. 资源泄漏（未关闭的流、连接等）
                5. 异常处理不当
                6. 代码规范和最佳实践
                7. 性能问题

                请以JSON格式返回评审结果：
                {
                  "summary": "总体评价（50字以内）",
                  "score": 评分(1-10),
                  "issues": [
                    {
                      "severity": "CRITICAL/HIGH/MEDIUM/LOW",
                      "category": "问题类别",
                      "line": "相关代码行",
                      "description": "问题描述",
                      "suggestion": "修复建议"
                    }
                  ]
                }
                只返回JSON，不要包含任何其他内容。""";

        String userMsg = "语言: " + (language != null ? language : "Java") + "\n代码:\n```\n" + code + "\n```";

        String result = callLLM(provider, systemPrompt, userMsg);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("language", language != null ? language : "Java");
        response.put("code", code);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(result, Map.class);
            response.put("review", parsed);
            response.put("rawResponse", result);
        } catch (Exception e) {
            response.put("review", Map.of("summary", "AI评审解析失败", "score", 0, "issues", List.of()));
            response.put("rawResponse", result);
        }
        response.put("model", provider.getModel());
        return response;
    }

    private String callLLM(AiModelProvider provider, String systemPrompt, String userMessage) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", provider.getModel(),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userMessage)
                    ),
                    "max_tokens", 2000,
                    "temperature", 0.1
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = provider.getRestClient().post()
                    .uri("/v1/chat/completions")
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("choices")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
            return "{\"summary\": \"AI 响应异常\", \"score\": 0, \"issues\": []}";
        } catch (Exception e) {
            log.error("Code review LLM call failed", e);
            return "{\"summary\": \"AI 调用失败: " + e.getMessage() + "\", \"score\": 0, \"issues\": []}";
        }
    }
}
