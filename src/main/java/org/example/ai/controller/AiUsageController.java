package org.example.ai.controller;

import org.example.ai.service.AiUsageService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/monitor/ai-usage")
public class AiUsageController {

    private final AiUsageService usageService;

    public AiUsageController(AiUsageService usageService) {
        this.usageService = usageService;
    }

    @GetMapping
    public Map<String, Object> page(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size,
                                     @RequestParam(required = false) String model,
                                     @RequestParam(required = false) String username) {
        var result = usageService.page(page, size, model, username);
        return Map.of(
                "success", true,
                "data", result.getRecords(),
                "pagination", Map.of(
                        "page", result.getCurrent(),
                        "size", result.getSize(),
                        "total", result.getTotal(),
                        "pages", result.getPages()
                ),
                "timestamp", System.currentTimeMillis()
        );
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return Map.of(
                "success", true,
                "data", usageService.getSummary(),
                "timestamp", System.currentTimeMillis()
        );
    }

    @DeleteMapping
    public Map<String, Object> clean(@RequestParam(defaultValue = "90") int beforeDays) {
        int count = usageService.clean(beforeDays);
        return Map.of(
                "success", true,
                "message", "已清理 " + beforeDays + " 天前的 AI 用量记录，共 " + count + " 条",
                "deletedCount", count,
                "timestamp", System.currentTimeMillis()
        );
    }
}
