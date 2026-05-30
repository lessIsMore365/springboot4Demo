package org.example.ai.controller;

import org.example.ai.service.RagService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/ai/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @GetMapping("/kb")
    public Map<String, Object> listKbs() {
        return Map.of(
                "success", true,
                "data", ragService.listKbs(),
                "timestamp", System.currentTimeMillis()
        );
    }

    @PostMapping("/kb")
    public Map<String, Object> createKb(@RequestBody Map<String, String> body) {
        var kb = ragService.createKb(body.get("name"), body.get("description"));
        return Map.of(
                "success", true,
                "data", kb,
                "message", "知识库创建成功",
                "timestamp", System.currentTimeMillis()
        );
    }

    @DeleteMapping("/kb/{kbId}")
    public Map<String, Object> deleteKb(@PathVariable Long kbId) {
        ragService.deleteKb(kbId);
        return Map.of(
                "success", true,
                "message", "知识库已删除",
                "timestamp", System.currentTimeMillis()
        );
    }

    @GetMapping("/kb/{kbId}/docs")
    public Map<String, Object> listDocs(@PathVariable Long kbId) {
        return Map.of(
                "success", true,
                "data", ragService.listDocs(kbId),
                "timestamp", System.currentTimeMillis()
        );
    }

    @PostMapping(value = "/kb/{kbId}/docs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadDoc(@PathVariable Long kbId, @RequestParam("file") MultipartFile file) {
        var doc = ragService.uploadDoc(kbId, file);
        return Map.of(
                "success", true,
                "data", doc,
                "message", "文档上传并解析成功，共 " + doc.getChunkCount() + " 个分块",
                "timestamp", System.currentTimeMillis()
        );
    }

    @DeleteMapping("/docs/{docId}")
    public Map<String, Object> deleteDoc(@PathVariable Long docId) {
        ragService.deleteDoc(docId);
        return Map.of(
                "success", true,
                "message", "文档已删除",
                "timestamp", System.currentTimeMillis()
        );
    }

    @PostMapping("/kb/{kbId}/ask")
    public Map<String, Object> ask(@PathVariable Long kbId,
                                    @RequestBody Map<String, String> body) {
        String question = body.get("question");
        String provider = body.getOrDefault("provider", "deepseek");
        if (question == null || question.isBlank()) {
            return Map.of("error", "question is required");
        }
        var result = ragService.ask(kbId, question, provider);
        return Map.of(
                "success", true,
                "data", result,
                "timestamp", System.currentTimeMillis()
        );
    }
}
