package org.example.ai.function;

import org.example.ai.model.ChatRequest;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AiFunctionRegistry {

    private final Map<String, AiFunction> functions = new ConcurrentHashMap<>();

    public AiFunctionRegistry(List<AiFunction> functionList) {
        for (AiFunction fn : functionList) {
            functions.put(fn.name(), fn);
        }
    }

    public void register(AiFunction fn) {
        functions.put(fn.name(), fn);
    }

    public AiFunction get(String name) {
        return functions.get(name);
    }

    public Map<String, AiFunction> getAll() {
        return Map.copyOf(functions);
    }

    public List<ChatRequest.Tool> getToolDefinitions() {
        return functions.values().stream()
                .map(fn -> new ChatRequest.Tool(
                        "function",
                        new ChatRequest.Tool.Function(
                                fn.name(),
                                fn.description(),
                                fn.parameters()
                        )
                ))
                .toList();
    }

    public Map<String, Object> getFunctionList() {
        List<Map<String, Object>> list = functions.values().stream()
                .map(fn -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", fn.name());
                    m.put("description", fn.description());
                    m.put("parameters", fn.parameters());
                    return m;
                })
                .toList();
        return Map.of("count", list.size(), "functions", list);
    }
}
