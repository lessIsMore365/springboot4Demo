package org.example.ai.provider;

import org.springframework.web.client.RestClient;

import java.util.Map;

public interface AiModelProvider {

    String getName();

    String getDisplayName();

    String getModel();

    RestClient getRestClient();

    double getCostPerMillionTokens();

    default int getMaxTokens() { return 4096; }

    default double getTemperature() { return 0.7; }

    default Map<String, Object> getInfo() {
        return Map.of(
                "name", getName(),
                "displayName", getDisplayName(),
                "model", getModel(),
                "costPerMillionTokens", getCostPerMillionTokens()
        );
    }
}
