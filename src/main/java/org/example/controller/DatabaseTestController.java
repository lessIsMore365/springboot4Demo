package org.example.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/db")
public class DatabaseTestController {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DatabaseTestController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/test")
    public Map<String, Object> testConnection() {
        // 打印当前线程信息（确认是否是虚拟线程）
        Thread currentThread = Thread.currentThread();
        System.out.println("Database test executing in thread: " + currentThread);
        System.out.println("Thread is virtual: " + currentThread.isVirtual());

        // 执行简单的 PostgreSQL 版本查询
        String version = jdbcTemplate.queryForObject("SELECT version()", String.class);

        // 测试数据库连接和基本查询
        Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);

        return Map.of(
            "status", "success",
            "databaseVersion", version,
            "testQueryResult", one,
            "threadInfo", Map.of(
                "name", currentThread.getName(),
                "isVirtual", currentThread.isVirtual(),
                "threadId", currentThread.threadId()
            ),
            "message", "PostgreSQL database connection successful with virtual thread support"
        );
    }

    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        try {
            // 简单健康检查
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return Map.of(
                "status", "UP",
                "database", "PostgreSQL",
                "message", "Database connection is healthy"
            );
        } catch (Exception e) {
            return Map.of(
                "status", "DOWN",
                "database", "PostgreSQL",
                "error", e.getMessage(),
                "message", "Database connection failed"
            );
        }
    }
}