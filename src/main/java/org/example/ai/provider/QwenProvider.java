package org.example.ai.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "ai.qwen.api-key", matchIfMissing = false)
public class QwenProvider implements AiModelProvider {

    private final RestClient restClient;

    @Value("${ai.qwen.model:qwen-turbo}")
    private String model;

    public QwenProvider(
            @Value("${ai.qwen.api-key:}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override public String getName() { return "qwen"; }
    @Override public String getDisplayName() { return "通义千问"; }
    @Override public String getModel() { return model; }
    @Override public RestClient getRestClient() { return restClient; }
    @Override public double getCostPerMillionTokens() { return 2.0; }
}
