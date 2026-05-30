package org.example.ai.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "ai.kimi.api-key", matchIfMissing = false)
public class KimiProvider implements AiModelProvider {

    private final RestClient restClient;

    @Value("${ai.kimi.model:moonshot-v1-8k}")
    private String model;

    public KimiProvider(
            @Value("${ai.kimi.api-key:}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.moonshot.cn")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override public String getName() { return "kimi"; }
    @Override public String getDisplayName() { return "Kimi (月之暗面)"; }
    @Override public String getModel() { return model; }
    @Override public RestClient getRestClient() { return restClient; }
    @Override public double getCostPerMillionTokens() { return 12.0; }
}
