package org.example.ai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatRequest(
        String model,
        List<ChatMessage> messages,
        Boolean stream,
        @JsonProperty("max_tokens") Integer maxTokens,
        Double temperature,
        List<Tool> tools,
        @JsonProperty("tool_choice") String toolChoice
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Tool(
            String type,
            Function function
    ) {
        public record Function(
                String name,
                String description,
                Object parameters
        ) {}
    }
}
