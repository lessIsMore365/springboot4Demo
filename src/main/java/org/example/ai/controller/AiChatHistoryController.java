package org.example.ai.controller;

import org.example.ai.service.AiChatHistoryService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai/history")
public class AiChatHistoryController {

    private final AiChatHistoryService historyService;

    public AiChatHistoryController(AiChatHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/sessions")
    public Map<String, Object> listSessions(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) String username) {
        var result = historyService.listSessions(page, size, username);
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

    @GetMapping("/sessions/{sessionId}")
    public Map<String, Object> getHistory(@PathVariable String sessionId) {
        var session = historyService.getSession(sessionId);
        if (session == null) {
            return Map.of("success", false, "message", "会话不存在");
        }
        var messages = historyService.getHistory(sessionId);
        return Map.of(
                "success", true,
                "data", Map.of("session", session, "messages", messages),
                "timestamp", System.currentTimeMillis()
        );
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Map<String, Object> deleteSession(@PathVariable String sessionId) {
        historyService.deleteSession(sessionId);
        return Map.of(
                "success", true,
                "message", "会话已删除",
                "timestamp", System.currentTimeMillis()
        );
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return Map.of(
                "success", true,
                "data", historyService.getStats(),
                "timestamp", System.currentTimeMillis()
        );
    }
}
