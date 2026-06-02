package org.example.ai.provider;

import org.example.ai.service.AiProviderConfigService;
import org.example.entity.AiProviderConfig;
import org.springframework.web.client.RestClient;

public abstract class BaseAiProvider implements AiModelProvider {

    private final AiProviderConfigService configService;

    protected BaseAiProvider(AiProviderConfigService configService) {
        this.configService = configService;
    }

    @Override
    public String getModel() {
        return configService.getConfig(getName()).getModel();
    }

    @Override
    public RestClient getRestClient() {
        return configService.getRestClient(getName());
    }

    @Override
    public double getCostPerMillionTokens() {
        return configService.getConfig(getName()).getCostPerMillionTokens().doubleValue();
    }

    @Override
    public int getMaxTokens() {
        return configService.getConfig(getName()).getMaxTokens();
    }

    @Override
    public double getTemperature() {
        return configService.getConfig(getName()).getTemperature().doubleValue();
    }

    protected AiProviderConfig getMyConfig() {
        return configService.getConfig(getName());
    }
}
