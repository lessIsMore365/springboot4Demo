package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.service.DatabaseMonitorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 数据库监控控制器
 * 提供连接池、表统计、延迟测试等实时监控 API
 */
@RestController
@RequestMapping("/api/monitor/db")
@RequiredArgsConstructor
public class DatabaseMonitorController {

    private final DatabaseMonitorService dbMonitorService;

    /**
     * 数据库综合概览 — DB 元信息 + 连接池状态 + 健康检查
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> overview() {
        DatabaseMonitorService.DatabaseOverview data = dbMonitorService.getOverview();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", data,
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * 连接池详情 — 活跃/空闲/等待/累计统计
     */
    @GetMapping("/pool")
    public ResponseEntity<Map<String, Object>> pool() {
        DatabaseMonitorService.ConnectionPoolDetail data = dbMonitorService.getConnectionPoolDetail();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", data,
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * 表统计 — 行数、空间占用、索引、扫描/增删改统计
     */
    @GetMapping("/tables")
    public ResponseEntity<Map<String, Object>> tables() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", dbMonitorService.getTableStats(),
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * 连接延迟测试 — 获取连接 + 有效性验证耗时
     */
    @GetMapping("/latency")
    public ResponseEntity<Map<String, Object>> latency() {
        DatabaseMonitorService.ConnectionLatency data = dbMonitorService.testConnectionLatency();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", data,
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * 数据库健康检查（公开）
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "status", "UP",
                        "service", "数据库监控服务",
                        "timestamp", System.currentTimeMillis()
                ),
                "timestamp", System.currentTimeMillis()
        ));
    }
}
