package org.example.ai.provider;

import org.example.ai.service.AiProviderConfigService;
import org.springframework.stereotype.Component;

@Component
public class DeepSeekProvider extends BaseAiProvider {

    public DeepSeekProvider(AiProviderConfigService configService) {
        super(configService);
    }

    @Override public String getName() { return "deepseek"; }
    @Override public String getDisplayName() { return "DeepSeek"; }
}
