package org.example.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.entity.AiProviderConfig;
import org.example.mapper.AiProviderConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiProviderConfigService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AiProviderConfigService.class);

    private final AiProviderConfigMapper mapper;
    private final Map<String, AiProviderConfig> configCache = new ConcurrentHashMap<>();
    private final Map<String, RestClient> restClientCache = new ConcurrentHashMap<>();

    public AiProviderConfigService(AiProviderConfigMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedDefaults();
        loadFromDb();
        applyEnvApiKeys();
    }

    // ========== 缓存加载 ==========

    public void loadFromDb() {
        List<AiProviderConfig> list = mapper.selectList(null);
        configCache.clear();
        restClientCache.clear();
        for (AiProviderConfig cfg : list) {
            configCache.put(cfg.getName(), cfg);
        }
        log.info("Loaded {} AI provider configs from database", list.size());
    }

    public void seedDefaults() {
        if (mapper.selectCount(null) > 0) return;

        List<AiProviderConfig> defaults = List.of(
                buildConfig("deepseek", "DeepSeek", "https://api.deepseek.com", "deepseek-chat", 4096, 0.7, 1.0, 1),
                buildConfig("qwen", "通义千问", "https://dashscope.aliyuncs.com/compatible-mode", "qwen-turbo", 4096, 0.7, 2.0, 2),
                buildConfig("kimi", "Kimi (月之暗面)", "https://api.moonshot.cn", "moonshot-v1-8k", 4096, 0.7, 12.0, 3),
                buildConfig("glm", "智谱 GLM", "https://open.bigmodel.cn/api/paas/v4", "glm-4-flash", 4096, 0.7, 5.0, 4)
        );
        for (AiProviderConfig cfg : defaults) {
            mapper.insert(cfg);
        }
        log.info("Seeded {} default AI provider configs", defaults.size());
    }

    private AiProviderConfig buildConfig(String name, String displayName, String baseUrl,
                                          String model, int maxTokens, double temperature,
                                          double cost, int sortOrder) {
        AiProviderConfig cfg = new AiProviderConfig();
        cfg.setName(name);
        cfg.setDisplayName(displayName);
        cfg.setApiKey("");
        cfg.setBaseUrl(baseUrl);
        cfg.setModel(model);
        cfg.setMaxTokens(maxTokens);
        cfg.setTemperature(BigDecimal.valueOf(temperature));
        cfg.setCostPerMillionTokens(BigDecimal.valueOf(cost));
        cfg.setEnabled(true);
        cfg.setSortOrder(sortOrder);
        return cfg;
    }

    // ========== 查询 ==========

    public AiProviderConfig getConfig(String name) {
        AiProviderConfig cfg = configCache.get(name);
        if (cfg == null) {
            throw new IllegalArgumentException("未知的模型提供商: " + name);
        }
        if (Boolean.FALSE.equals(cfg.getEnabled())) {
            throw new IllegalArgumentException("模型提供商已禁用: " + name);
        }
        return cfg;
    }

    public Optional<AiProviderConfig> getConfigQuietly(String name) {
        return Optional.ofNullable(configCache.get(name));
    }

    public List<AiProviderConfig> listAll() {
        return configCache.values().stream()
                .sorted(Comparator.comparing(AiProviderConfig::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public List<AiProviderConfig> listEnabled() {
        return configCache.values().stream()
                .filter(c -> Boolean.TRUE.equals(c.getEnabled()))
                .sorted(Comparator.comparing(AiProviderConfig::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    // ========== RestClient 管理 ==========

    public RestClient getRestClient(String name) {
        AiProviderConfig cfg = getConfig(name);
        String cacheKey = restClientKey(cfg);
        return restClientCache.computeIfAbsent(cacheKey, k -> buildRestClient(cfg));
    }

    private String restClientKey(AiProviderConfig cfg) {
        return cfg.getName() + "|" + cfg.getApiKey() + "|" + cfg.getBaseUrl();
    }

    private RestClient buildRestClient(AiProviderConfig cfg) {
        return RestClient.builder()
                .baseUrl(cfg.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + cfg.getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private void evictRestClient(String name) {
        restClientCache.keySet().removeIf(k -> k.startsWith(name + "|"));
    }

    // ========== 更新（实时生效） ==========

    public AiProviderConfig updateConfig(String name, AiProviderConfig dto) {
        AiProviderConfig existing = configCache.get(name);
        if (existing == null) {
            throw new IllegalArgumentException("未知的模型提供商: " + name);
        }

        if (dto.getApiKey() != null) existing.setApiKey(dto.getApiKey());
        if (dto.getBaseUrl() != null) existing.setBaseUrl(dto.getBaseUrl());
        if (dto.getModel() != null) existing.setModel(dto.getModel());
        if (dto.getDisplayName() != null) existing.setDisplayName(dto.getDisplayName());
        if (dto.getMaxTokens() != null) existing.setMaxTokens(dto.getMaxTokens());
        if (dto.getTemperature() != null) existing.setTemperature(dto.getTemperature());
        if (dto.getCostPerMillionTokens() != null) existing.setCostPerMillionTokens(dto.getCostPerMillionTokens());
        if (dto.getEnabled() != null) existing.setEnabled(dto.getEnabled());
        if (dto.getSortOrder() != null) existing.setSortOrder(dto.getSortOrder());

        mapper.updateById(existing);
        configCache.put(name, existing);
        evictRestClient(name);

        log.info("AI provider config updated: {} (model={}, enabled={})", name, existing.getModel(), existing.getEnabled());
        return existing;
    }

    public void refresh(String name) {
        AiProviderConfig dbCfg = mapper.selectOne(
                new LambdaQueryWrapper<AiProviderConfig>().eq(AiProviderConfig::getName, name));
        if (dbCfg != null) {
            configCache.put(name, dbCfg);
            evictRestClient(name);
            log.info("AI provider config refreshed: {}", name);
        }
    }

    public void refreshAll() {
        loadFromDb();
        log.info("All AI provider configs refreshed");
    }

    // ========== API Key 环境变量覆盖 ==========

    public void applyEnvApiKeys() {
        applyEnvKey("deepseek", "DEEPSEEK_API_KEY");
        applyEnvKey("qwen", "QWEN_API_KEY");
        applyEnvKey("kimi", "KIMI_API_KEY");
        applyEnvKey("glm", "GLM_API_KEY");
    }

    private void applyEnvKey(String name, String envVar) {
        String envValue = System.getenv(envVar);
        if (envValue != null && !envValue.isBlank()) {
            AiProviderConfig cfg = configCache.get(name);
            if (cfg != null && (cfg.getApiKey() == null || cfg.getApiKey().isBlank())) {
                cfg.setApiKey(envValue);
                mapper.updateById(cfg);
                evictRestClient(name);
                log.info("Applied env {} for provider {}", envVar, name);
            }
        }
    }

    // ========== 脱敏 ==========

    public String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) return "****";
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
