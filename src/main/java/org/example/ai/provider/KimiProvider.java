package org.example.ai.provider;

import org.example.ai.service.AiProviderConfigService;
import org.springframework.stereotype.Component;

@Component
public class KimiProvider extends BaseAiProvider {

    public KimiProvider(AiProviderConfigService configService) {
        super(configService);
    }

    @Override public String getName() { return "kimi"; }
    @Override public String getDisplayName() { return "Kimi (月之暗面)"; }
}
