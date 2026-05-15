package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.service.ServerMonitorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 服务器监控控制器
 * 提供操作系统级指标：CPU、内存、磁盘、网络、进程
 */
@RestController
@RequestMapping("/api/monitor/server")
@RequiredArgsConstructor
public class ServerMonitorController {

    private final ServerMonitorService serverMonitorService;

    /**
     * 服务器综合概览 — OS + CPU + 内存 + 磁盘 + 网络 + 进程
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> overview() {
        ServerMonitorService.ServerOverview data = serverMonitorService.getOverview();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", data,
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * CPU 详情 — 使用率 + 负载 + 每核负载
     */
    @GetMapping("/cpu")
    public ResponseEntity<Map<String, Object>> cpu() {
        ServerMonitorService.CpuDetail data = serverMonitorService.getCpuDetail();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", data,
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * 内存详情 — 物理内存 + Swap + JVM 堆
     */
    @GetMapping("/memory")
    public ResponseEntity<Map<String, Object>> memory() {
        ServerMonitorService.MemoryDetail data = serverMonitorService.getMemoryDetail();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", data,
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * 磁盘详情 — 各分区空间和使用率
     */
    @GetMapping("/disk")
    public ResponseEntity<Map<String, Object>> disk() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", serverMonitorService.getDiskDetail(),
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * 网络详情 — 各接口流量统计
     */
    @GetMapping("/network")
    public ResponseEntity<Map<String, Object>> network() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", serverMonitorService.getNetworkDetail(),
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * 服务器健康检查（公开）
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "status", "UP",
                        "service", "服务器监控服务",
                        "timestamp", System.currentTimeMillis()
                ),
                "timestamp", System.currentTimeMillis()
        ));
    }
}
