package org.example.ai.provider;

import org.springframework.web.client.RestClient;

import java.util.Map;

public interface AiModelProvider {

    String getName();

    String getDisplayName();

    String getModel();

    RestClient getRestClient();

    double getCostPerMillionTokens();

    default Map<String, Object> getInfo() {
        return Map.of(
                "name", getName(),
                "displayName", getDisplayName(),
                "model", getModel(),
                "costPerMillionTokens", getCostPerMillionTokens()
        );
    }
}
