package org.example.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ai.function.AiFunction;
import org.example.ai.function.AiFunctionRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class McpController {

    private static final Logger log = LoggerFactory.getLogger(McpController.class);

    private final AiFunctionRegistry functionRegistry;
    private final ObjectMapper objectMapper;

    public McpController(AiFunctionRegistry functionRegistry, ObjectMapper objectMapper) {
        this.functionRegistry = functionRegistry;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/.well-known/mcp")
    public Map<String, Object> serverInfo() {
        return Map.of(
                "protocol", "mcp",
                "version", "0.1.0",
                "name", "springboot4Demo MCP Server",
                "description", "MCP Server exposing Spring Boot 4 demo business functions",
                "endpoint", "/api/mcp"
        );
    }

    @PostMapping("/api/mcp")
    public Map<String, Object> handleJsonRpc(@RequestBody Map<String, Object> request) {
        String method = (String) request.get("method");
        Object id = request.getOrDefault("id", System.currentTimeMillis());

        try {
            return switch (method) {
                case "initialize" -> handleInitialize(id);
                case "tools/list" -> handleToolsList(id);
                case "tools/call" -> handleToolsCall(id, request);
                case "ping" -> Map.of("jsonrpc", "2.0", "id", id, "result", Map.of());
                default -> errorResponse(id, -32601, "Method not found: " + method);
            };
        } catch (Exception e) {
            log.error("MCP error for method {}: {}", method, e.getMessage());
            return errorResponse(id, -32603, e.getMessage());
        }
    }

    private Map<String, Object> handleInitialize(Object id) {
        Map<String, Object> capabilities = Map.of(
                "tools", Map.of("listChanged", false)
        );
        Map<String, Object> serverInfo = Map.of(
                "name", "springboot4Demo",
                "version", "1.0.0"
        );
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("protocolVersion", "0.1.0");
        result.put("capabilities", capabilities);
        result.put("serverInfo", serverInfo);
        return Map.of("jsonrpc", "2.0", "id", id, "result", result);
    }

    private Map<String, Object> handleToolsList(Object id) {
        List<Map<String, Object>> tools = new ArrayList<>();
        for (AiFunction fn : functionRegistry.getAll().values()) {
            Map<String, Object> tool = new LinkedHashMap<>();
            tool.put("name", fn.name());
            tool.put("description", fn.description());
            tool.put("inputSchema", fn.parameters());
            tools.add(tool);
        }
        return Map.of("jsonrpc", "2.0", "id", id, "result", Map.of("tools", tools));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> handleToolsCall(Object id, Map<String, Object> request) {
        Map<String, Object> params = (Map<String, Object>) request.get("params");
        String toolName = (String) params.get("name");
        Map<String, Object> arguments = (Map<String, Object>) params.getOrDefault("arguments", Map.of());

        AiFunction fn = functionRegistry.get(toolName);
        if (fn == null) {
            return errorResponse(id, -32602, "Unknown tool: " + toolName);
        }

        Object result = fn.execute(arguments);
        List<Map<String, Object>> content = new ArrayList<>();
        try {
            String text = result instanceof String ? (String) result : objectMapper.writeValueAsString(result);
            content.add(Map.of("type", "text", "text", text));
        } catch (Exception e) {
            content.add(Map.of("type", "text", "text", "Error: " + e.getMessage()));
        }

        return Map.of("jsonrpc", "2.0", "id", id, "result", Map.of("content", content));
    }

    private Map<String, Object> errorResponse(Object id, int code, String message) {
        return Map.of(
                "jsonrpc", "2.0",
                "id", id,
                "error", Map.of("code", code, "message", message)
        );
    }
}
