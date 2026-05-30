package org.example.ai.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "ai.glm.api-key", matchIfMissing = false)
public class GlmProvider implements AiModelProvider {

    private final RestClient restClient;

    @Value("${ai.glm.model:glm-4-flash}")
    private String model;

    public GlmProvider(
            @Value("${ai.glm.api-key:}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl("https://open.bigmodel.cn/api/paas/v4")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override public String getName() { return "glm"; }
    @Override public String getDisplayName() { return "智谱 GLM"; }
    @Override public String getModel() { return model; }
    @Override public RestClient getRestClient() { return restClient; }
    @Override public double getCostPerMillionTokens() { return 5.0; }
}
