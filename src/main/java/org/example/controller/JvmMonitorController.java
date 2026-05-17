package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.service.JvmMonitorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * JVM 监控控制器
 * 提供堆内存、线程（虚拟线程/平台线程）、GC 等实时监控 API
 */
@RestController
@RequestMapping("/api/monitor/jvm")
@RequiredArgsConstructor
public class JvmMonitorController {

    private final JvmMonitorService monitorService;

    /**
     * JVM 综合概览 - 一次调用获取全部关键指标（含 GC 告警）
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> overview() {
        JvmMonitorService.JvmOverview data = monitorService.getOverview();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", data,
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * 内存详情 - 堆/非堆内存 + 各内存池 + 物理内存
     */
    @GetMapping("/memory")
    public ResponseEntity<Map<String, Object>> memory() {
        JvmMonitorService.MemoryDetail data = monitorService.getMemoryDetail();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", data,
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * 线程详情 - 虚拟线程 vs 平台线程统计 + CPU Top 线程 + 状态分布
     */
    @GetMapping("/threads")
    public ResponseEntity<Map<String, Object>> threads() {
        JvmMonitorService.ThreadDetail data = monitorService.getThreadDetail();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", data,
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * 线程转储 - 所有线程及堆栈跟踪
     */
    @GetMapping("/thread-dump")
    public ResponseEntity<Map<String, Object>> threadDump() {
        JvmMonitorService.ThreadDump data = monitorService.getThreadDump();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", data,
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * GC 详情 - 各 GC 收集器详情、最近 GC 内存变化、异常告警
     */
    @GetMapping("/gc")
    public ResponseEntity<Map<String, Object>> gc() {
        JvmMonitorService.GcDetailResult data = monitorService.getGcDetail();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", data,
                "timestamp", System.currentTimeMillis()
        ));
    }

    /**
     * GC 事件历史 - 最近 GC 事件时间线 + Young/Full GC 分类统计（含暂停时间分布）
     */
    @GetMapping("/gc/history")
    public ResponseEntity<Map<String, Object>> gcHistory() {
        JvmMonitorService.GcHistory data = monitorService.getGcHistory();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", data,
                "timestamp", System.currentTimeMillis()
        ));
    }
}
