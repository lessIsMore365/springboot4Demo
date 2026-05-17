package org.example.java21demo.model;

/**
 * 用户上下文 - 用于 ScopedValue 演示
 */
public record UserContext(Long userId, String username, String role) {
}
