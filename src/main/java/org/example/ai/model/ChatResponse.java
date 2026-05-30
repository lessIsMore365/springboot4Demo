package org.example.ai.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ChatResponse(
        String id,
        String object,
        Long created,
        String model,
        List<Choice> choices,
        Usage usage
) {
    public record Choice(
            Integer index,
            Delta delta,
            Message message,
            @JsonProperty("finish_reason") String finishReason
    ) {}

    public record Delta(
            String role,
            String content,
            @JsonProperty("tool_calls") List<ToolCallDelta> toolCalls
    ) {}

    public record ToolCallDelta(
            Integer index,
            String id,
            String type,
            Function function
    ) {
        public record Function(
                String name,
                String arguments
        ) {}
    }

    public record Message(
            String role,
            String content,
            @JsonProperty("tool_calls") List<ChatMessage.ToolCall> toolCalls
    ) {}

    public record Usage(
            @JsonProperty("prompt_tokens") Integer promptTokens,
            @JsonProperty("completion_tokens") Integer completionTokens,
            @JsonProperty("total_tokens") Integer totalTokens
    ) {}
}
