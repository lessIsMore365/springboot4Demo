package org.example.java21demo;

import lombok.RequiredArgsConstructor;
import org.example.java21demo.model.UserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/java21/scoped-value")
@RequiredArgsConstructor
public class ScopedValueDemoController {

    private final ScopedValueDemoService service;

    @GetMapping("/basic")
    public Map<String, Object> basicUsage() {
        return service.basicUsage();
    }

    @GetMapping("/isolation")
    public Map<String, Object> scopeIsolation() {
        return service.scopeIsolation();
    }

    @GetMapping("/request-context")
    public Map<String, Object> requestContext(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(defaultValue = "admin") String username,
            @RequestParam(defaultValue = "ROLE_ADMIN") String role) {
        UserContext user = new UserContext(userId, username, role);
        return service.requestContextDemo(user);
    }

    @GetMapping("/compare-tl")
    public Map<String, Object> compareWithThreadLocal() {
        return service.compareWithThreadLocal();
    }

    @GetMapping("/fallback")
    public Map<String, Object> fallback() {
        return service.fallbackMethods();
    }

    @GetMapping("/multi")
    public Map<String, Object> multiple() {
        return service.multipleBindings();
    }

    @GetMapping("/compare-traditional")
    public Map<String, Object> compareTraditional() {
        return service.compareTraditionalVsScoped();
    }
}
