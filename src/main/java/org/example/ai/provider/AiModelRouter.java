package org.example.ai.provider;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AiModelRouter {

    private final Map<String, AiModelProvider> providers = new ConcurrentHashMap<>();
    private final String defaultProvider;

    public AiModelRouter(List<AiModelProvider> providerList) {
        for (AiModelProvider p : providerList) {
            providers.put(p.getName(), p);
        }
        this.defaultProvider = !providerList.isEmpty() ? providerList.get(0).getName() : "deepseek";
    }

    public AiModelProvider resolve(String providerName) {
        if (providerName == null || providerName.isBlank()) {
            return providers.get(defaultProvider);
        }
        AiModelProvider p = providers.get(providerName);
        if (p == null) {
            throw new IllegalArgumentException("未知的模型提供商: " + providerName + "，可用: " + providers.keySet());
        }
        return p;
    }

    public AiModelProvider getDefault() {
        return providers.get(defaultProvider);
    }

    public Map<String, Object> getAvailableProviders() {
        List<Map<String, Object>> list = providers.values().stream()
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", p.getName());
                    m.put("displayName", p.getDisplayName());
                    m.put("model", p.getModel());
                    m.put("costPerMillionTokens", p.getCostPerMillionTokens());
                    return m;
                })
                .toList();
        return Map.of("count", list.size(), "defaultProvider", defaultProvider, "providers", list);
    }
}
