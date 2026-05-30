package org.example.ai.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class DeepSeekProvider implements AiModelProvider {

    private final RestClient restClient;

    @Value("${ai.deepseek.model}")
    private String model;

    public DeepSeekProvider(
            @Value("${ai.deepseek.api-key}") String apiKey,
            @Value("${ai.deepseek.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override public String getName() { return "deepseek"; }
    @Override public String getDisplayName() { return "DeepSeek"; }
    @Override public String getModel() { return model; }
    @Override public RestClient getRestClient() { return restClient; }
    @Override public double getCostPerMillionTokens() { return 1.0; }
}
