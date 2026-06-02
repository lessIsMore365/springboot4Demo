package org.example.ai.provider;

import org.example.ai.service.AiProviderConfigService;
import org.example.entity.AiProviderConfig;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AiModelRouter {

    private final Map<String, AiModelProvider> providers = new ConcurrentHashMap<>();
    private final AiProviderConfigService configService;

    public AiModelRouter(List<AiModelProvider> providerList, AiProviderConfigService configService) {
        this.configService = configService;
        for (AiModelProvider p : providerList) {
            providers.put(p.getName(), p);
        }
    }

    public AiModelProvider resolve(String providerName) {
        String name = (providerName == null || providerName.isBlank())
                ? getDefaultName()
                : providerName;

        AiModelProvider p = providers.get(name);
        if (p == null) {
            throw new IllegalArgumentException("未知的模型提供商: " + name + "，可用: " + getEnabledNames());
        }
        return p;
    }

    public AiModelProvider getDefault() {
        return providers.get(getDefaultName());
    }

    private String getDefaultName() {
        List<AiProviderConfig> enabled = configService.listEnabled();
        if (enabled.isEmpty()) {
            throw new IllegalStateException("没有启用的 AI 模型提供商");
        }
        return enabled.get(0).getName();
    }

    private List<String> getEnabledNames() {
        return configService.listEnabled().stream().map(AiProviderConfig::getName).toList();
    }

    public Map<String, Object> getAvailableProviders() {
        List<Map<String, Object>> list = configService.listEnabled().stream()
                .map(cfg -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", cfg.getName());
                    m.put("displayName", cfg.getDisplayName());
                    m.put("model", cfg.getModel());
                    m.put("costPerMillionTokens", cfg.getCostPerMillionTokens());
                    return m;
                })
                .toList();
        return Map.of("count", list.size(), "defaultProvider", getDefaultName(), "providers", list);
    }
}
