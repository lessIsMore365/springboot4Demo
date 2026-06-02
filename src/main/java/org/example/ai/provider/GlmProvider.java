package org.example.ai.provider;

import org.example.ai.service.AiProviderConfigService;
import org.springframework.stereotype.Component;

@Component
public class GlmProvider extends BaseAiProvider {

    public GlmProvider(AiProviderConfigService configService) {
        super(configService);
    }

    @Override public String getName() { return "glm"; }
    @Override public String getDisplayName() { return "智谱 GLM"; }
}
