package org.example.service;

import java.util.List;
import java.util.Map;

/**
 * JVM 监控服务接口
 * 提供堆内存、线程（虚拟线程/平台线程）、GC 等实时监控数据
 */
public interface JvmMonitorService {

    /** 获取 JVM 综合概览（一次调用获取全部关键指标） */
    JvmOverview getOverview();

    /** 获取内存详情（含各内存池） */
    MemoryDetail getMemoryDetail();

    /** 获取线程详情（虚拟线程 vs 平台线程） */
    ThreadDetail getThreadDetail();

    /** 获取 GC 详情（含异常检测） */
    GcDetailResult getGcDetail();

    /** 获取线程转储（所有线程及堆栈） */
    ThreadDump getThreadDump();

    /** 获取 GC 事件历史（Young/Full GC 事件 + 暂停时间分布） */
    GcHistory getGcHistory();

    // ==================== 数据模型 ====================

    record JvmOverview(
            String jvmName,
            String jvmVersion,
            long uptimeMs,
            String uptimeFormatted,
            int availableProcessors,
            double systemLoadAverage,
            MemorySummary memory,
            ThreadSummary threads,
            GcSummary gc
    ) {}

    record MemorySummary(
            long heapUsed,
            long heapMax,
            long heapCommitted,
            double heapUsagePercent,
            long nonHeapUsed,
            long nonHeapCommitted,
            long metaspaceUsed,
            long metaspaceMax,
            List<MemoryPoolInfo> pools
    ) {}

    record MemoryPoolInfo(
            String name,
            long used,
            long committed,
            long max,
            double usagePercent
    ) {}

    record MemoryDetail(
            MemorySummary summary,
            MemorySnapshot snapshot
    ) {}

    record MemorySnapshot(
            long totalPhysical,
            long freePhysical,
            long totalSwap,
            long freeSwap
    ) {}

    record ThreadSummary(
            int currentCount,
            int virtualCount,
            int platformCount,
            int daemonCount,
            int peakCount,
            long totalStarted
    ) {}

    record ThreadDetail(
            ThreadSummary summary,
            List<ThreadItem> topCpuThreads,
            List<ThreadStateCount> stateDistribution
    ) {}

    record ThreadItem(
            long id,
            String name,
            String state,
            boolean virtual,
            long cpuTimeMs,
            String stackTrace
    ) {}

    record ThreadStateCount(
            String state,
            int count
    ) {}

    // ==================== GC 数据模型 ====================

    /** GC 综合结果（详情 + 告警） */
    record GcDetailResult(
            List<GcCollectorInfo> collectors,
            GcWarning warnings,
            long timestamp
    ) {}

    /** Overview 中的 GC 摘要（含告警） */
    record GcSummary(
            List<GcCollectorInfo> collectors,
            GcWarning warnings
    ) {}

    /** 单个 GC 收集器详情 */
    record GcCollectorInfo(
            String name,
            long collectionCount,
            long collectionTimeMs,
            double avgTimeMs,
            double collectionsPerHour,
            List<String> memoryPoolNames,
            LastGcSnapshot lastGc
    ) {}

    /** 最近一次 GC 快照 */
    record LastGcSnapshot(
            long id,
            long startTime,
            long endTime,
            long durationMs,
            long elapsedSinceMs,
            Map<String, MemoryPoolDelta> pools
    ) {}

    /** 单内存池 GC 前后变化 */
    record MemoryPoolDelta(
            long usedBefore,
            long usedAfter,
            long committed,
            long max,
            long freedBytes,
            double usageBeforePercent,
            double usageAfterPercent
    ) {}

    /** GC 异常告警 */
    record GcWarning(
            boolean hasWarning,
            String severity,
            List<String> warnings
    ) {}

    // ==================== GC 事件历史 ====================

    /** 单次 GC 事件 */
    record GcEvent(
            long id,
            String collectorName,
            String gcAction,
            String gcCause,
            long startTime,
            long endTime,
            long durationMs,
            long elapsedSinceJvmStartMs,
            Map<String, MemoryPoolDelta> pools,
            long recordedAt
    ) {}

    /** GC 事件历史（含分类统计） */
    record GcHistory(
            List<GcEvent> events,
            GcStatistics youngGcStats,
            GcStatistics fullGcStats,
            int totalYoungGc,
            int totalFullGc
    ) {}

    /** GC 分类统计（含暂停时间分布） */
    record GcStatistics(
            long count,
            long totalTimeMs,
            double avgTimeMs,
            long maxPauseMs,
            long minPauseMs,
            long p50PauseMs,
            long p95PauseMs,
            long p99PauseMs,
            long totalFreedBytes
    ) {}

    // ==================== Thread Dump ====================

    record ThreadDump(
            int totalCount,
            int virtualCount,
            int platformCount,
            List<ThreadItem> threads
    ) {}
}
