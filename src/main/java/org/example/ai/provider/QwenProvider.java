package org.example.ai.provider;

import org.example.ai.service.AiProviderConfigService;
import org.springframework.stereotype.Component;

@Component
public class QwenProvider extends BaseAiProvider {

    public QwenProvider(AiProviderConfigService configService) {
        super(configService);
    }

    @Override public String getName() { return "qwen"; }
    @Override public String getDisplayName() { return "通义千问"; }
}
