package org.example.ai.controller;

import org.example.ai.service.AiProviderConfigService;
import org.example.entity.AiProviderConfig;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/config")
public class AiProviderConfigController {

    private final AiProviderConfigService configService;

    public AiProviderConfigController(AiProviderConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    public Map<String, Object> list() {
        List<Map<String, Object>> list = configService.listAll().stream()
                .map(this::toMaskedMap)
                .toList();
        return Map.of("success", true, "data", list, "total", list.size(),
                "timestamp", System.currentTimeMillis());
    }

    @GetMapping("/{name}")
    public Map<String, Object> get(@PathVariable String name) {
        AiProviderConfig cfg = configService.getConfig(name);
        return Map.of("success", true, "data", toMaskedMap(cfg),
                "timestamp", System.currentTimeMillis());
    }

    @PutMapping("/{name}")
    public Map<String, Object> update(@PathVariable String name, @RequestBody AiProviderConfig dto) {
        AiProviderConfig updated = configService.updateConfig(name, dto);
        return Map.of("success", true, "data", toMaskedMap(updated),
                "message", "配置已更新，实时生效",
                "timestamp", System.currentTimeMillis());
    }

    @PostMapping("/{name}/refresh")
    public Map<String, Object> refresh(@PathVariable String name) {
        configService.refresh(name);
        return Map.of("success", true, "message", "配置已刷新",
                "timestamp", System.currentTimeMillis());
    }

    @PostMapping("/refresh-all")
    public Map<String, Object> refreshAll() {
        configService.refreshAll();
        return Map.of("success", true, "message", "全部配置已刷新",
                "timestamp", System.currentTimeMillis());
    }

    private Map<String, Object> toMaskedMap(AiProviderConfig cfg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", cfg.getId());
        m.put("name", cfg.getName());
        m.put("displayName", cfg.getDisplayName());
        m.put("apiKey", configService.maskApiKey(cfg.getApiKey()));
        m.put("baseUrl", cfg.getBaseUrl());
        m.put("model", cfg.getModel());
        m.put("maxTokens", cfg.getMaxTokens());
        m.put("temperature", cfg.getTemperature());
        m.put("costPerMillionTokens", cfg.getCostPerMillionTokens());
        m.put("enabled", cfg.getEnabled());
        m.put("sortOrder", cfg.getSortOrder());
        m.put("createTime", cfg.getCreateTime());
        m.put("updateTime", cfg.getUpdateTime());
        return m;
    }
}
