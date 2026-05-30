package org.example.ai.controller;

import org.example.ai.service.AiCodeReviewService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai/code-review")
public class AiCodeReviewController {

    private final AiCodeReviewService codeReviewService;

    public AiCodeReviewController(AiCodeReviewService codeReviewService) {
        this.codeReviewService = codeReviewService;
    }

    @PostMapping
    public Map<String, Object> review(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        String language = body.getOrDefault("language", "Java");
        String provider = body.getOrDefault("provider", "deepseek");

        if (code == null || code.isBlank()) {
            return Map.of("error", "code is required");
        }

        var result = codeReviewService.review(code, language, provider);
        return Map.of(
                "success", true,
                "data", result,
                "timestamp", System.currentTimeMillis()
        );
    }
}
