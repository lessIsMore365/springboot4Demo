package org.example.service.impl;

import com.sun.management.GcInfo;
import lombok.extern.slf4j.Slf4j;
import org.example.service.JvmMonitorService;
import org.springframework.stereotype.Service;

import java.lang.management.*;
import java.util.*;

/**
 * JVM 监控服务实现
 * 基于 java.lang.management MXBeans 采集实时指标
 */
@Slf4j
@Service
public class JvmMonitorServiceImpl implements JvmMonitorService {

    private static final int STACK_DEPTH = 50;
    private static final int TOP_CPU_THREADS = 20;

    // GC 异常检测阈值
    private static final double YOUNG_GC_PER_HOUR_WARN = 120;   // Young GC > 120次/小时
    private static final double FULL_GC_PER_HOUR_WARN = 5;      // Full GC > 5次/小时
    private static final double FULL_GC_PER_HOUR_SEVERE = 10;    // Full GC > 10次/小时
    private static final long YOUNG_GC_PAUSE_MS_WARN = 500;     // Young GC 单次平均 > 500ms
    private static final long FULL_GC_PAUSE_MS_WARN = 2000;     // Full GC 单次平均 > 2s
    private static final double GC_OVERHEAD_WARN = 10.0;        // GC 总耗时占 uptime > 10%
    private static final double GC_OVERHEAD_SEVERE = 20.0;      // GC 总耗时占 uptime > 20%
    private static final double OLD_GEN_USAGE_AFTER_GC_WARN = 85.0; // Full GC 后老年代仍 > 85%
    private static final long YOUNG_GC_AVG_MS_WARN = 200;       // Young GC 平均 > 200ms
    private static final long ELAPSED_SINCE_GC_MS_WARN = 600_000; // 最近一次 GC 距今 > 10分钟
    private static final long MIN_UPTIME_MS_FOR_GC_CHECK = 300_000; // JVM 启动 ≥ 5分钟才做频率检测

    @Override
    public JvmOverview getOverview() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        GcDetailResult gcDetail = getGcDetail();

        return new JvmOverview(
                runtime.getVmName(),
                runtime.getVmVersion(),
                runtime.getUptime(),
                formatUptime(runtime.getUptime()),
                os.getAvailableProcessors(),
                os.getSystemLoadAverage(),
                buildMemorySummary(),
                buildThreadSummary(threadBean),
                new GcSummary(gcDetail.collectors(), gcDetail.warnings())
        );
    }

    @Override
    public MemoryDetail getMemoryDetail() {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        long totalPhysical = 0, freePhysical = 0, totalSwap = 0, freeSwap = 0;

        if (os instanceof com.sun.management.OperatingSystemMXBean sunOs) {
            totalPhysical = sunOs.getTotalMemorySize();
            freePhysical = sunOs.getFreeMemorySize();
            totalSwap = sunOs.getTotalSwapSpaceSize();
            freeSwap = sunOs.getFreeSwapSpaceSize();
        }

        return new MemoryDetail(
                buildMemorySummary(),
                new MemorySnapshot(totalPhysical, freePhysical, totalSwap, freeSwap)
        );
    }

    @Override
    public ThreadDetail getThreadDetail() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] allThreads = threadBean.dumpAllThreads(false, false, STACK_DEPTH);
        long[] threadIds = threadBean.getAllThreadIds();

        // 获取 CPU 时间并排序
        Map<Long, Long> cpuTimeMap = new HashMap<>();
        if (threadBean.isThreadCpuTimeSupported() && threadBean.isThreadCpuTimeEnabled()) {
            for (long id : threadIds) {
                long cpuNs = threadBean.getThreadCpuTime(id);
                if (cpuNs > 0) {
                    cpuTimeMap.put(id, cpuNs / 1_000_000); // ns -> ms
                }
            }
        }

        // 构建线程列表
        List<ThreadItem> items = new ArrayList<>();
        Map<String, Integer> stateDist = new LinkedHashMap<>();

        for (ThreadInfo ti : allThreads) {
            boolean virtual = isVirtualThread(ti);
            String state = ti.getThreadState().name();
            stateDist.merge(state, 1, Integer::sum);

            items.add(new ThreadItem(
                    ti.getThreadId(),
                    ti.getThreadName(),
                    state,
                    virtual,
                    cpuTimeMap.getOrDefault(ti.getThreadId(), 0L),
                    formatStackTrace(ti.getStackTrace())
            ));
        }

        // 按 CPU 时间排序取 Top N
        List<ThreadItem> topCpu = items.stream()
                .sorted(Comparator.comparingLong(ThreadItem::cpuTimeMs).reversed())
                .limit(TOP_CPU_THREADS)
                .toList();

        // 状态分布
        List<ThreadStateCount> stateCounts = stateDist.entrySet().stream()
                .map(e -> new ThreadStateCount(e.getKey(), e.getValue()))
                .toList();

        return new ThreadDetail(buildThreadSummary(threadBean), topCpu, stateCounts);
    }

    @Override
    public GcDetailResult getGcDetail() {
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        double uptimeHours = uptimeMs / 3_600_000.0;
        if (uptimeHours < 0.01) uptimeHours = 0.01; // 防止除零

        List<GcCollectorInfo> collectors = new ArrayList<>();
        long totalGcTimeMs = 0;
        long totalGcCount = 0;

        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            String name = gc.getName();
            long count = gc.getCollectionCount();
            long timeMs = gc.getCollectionTime();
            double avgTimeMs = count > 0 ? (double) timeMs / count : 0;
            double collectionsPerHour = count / uptimeHours;
            String[] poolNames = gc.getMemoryPoolNames();
            List<String> pools = poolNames != null ? List.of(poolNames) : List.of();

            LastGcSnapshot lastGc = null;
            if (gc instanceof com.sun.management.GarbageCollectorMXBean sunGc) {
                GcInfo lastGcInfo = sunGc.getLastGcInfo();
                if (lastGcInfo != null) {
                    Map<String, MemoryPoolDelta> poolDeltas = new LinkedHashMap<>();
                    Map<String, MemoryUsage> before = lastGcInfo.getMemoryUsageBeforeGc();
                    Map<String, MemoryUsage> after = lastGcInfo.getMemoryUsageAfterGc();

                    for (String poolName : before.keySet()) {
                        MemoryUsage beforeUsage = before.get(poolName);
                        MemoryUsage afterUsage = after.get(poolName);
                        if (beforeUsage != null && afterUsage != null) {
                            long max = beforeUsage.getMax() > 0 ? beforeUsage.getMax() : beforeUsage.getCommitted();
                            poolDeltas.put(poolName, new MemoryPoolDelta(
                                    beforeUsage.getUsed(),
                                    afterUsage.getUsed(),
                                    beforeUsage.getCommitted(),
                                    max,
                                    beforeUsage.getUsed() - afterUsage.getUsed(),
                                    max > 0 ? (double) beforeUsage.getUsed() / max * 100 : 0,
                                    max > 0 ? (double) afterUsage.getUsed() / max * 100 : 0
                            ));
                        }
                    }

                    // GcInfo 时间戳均相对于 JVM 启动时刻，需统一换算
                    lastGc = new LastGcSnapshot(
                            lastGcInfo.getId(),
                            lastGcInfo.getStartTime(),
                            lastGcInfo.getEndTime(),
                            lastGcInfo.getDuration(),
                            uptimeMs - lastGcInfo.getEndTime(),
                            poolDeltas
                    );
                }
            }

            collectors.add(new GcCollectorInfo(name, count, timeMs, avgTimeMs, collectionsPerHour, pools, lastGc));

            totalGcTimeMs += timeMs;
            totalGcCount += count;
        }

        // 异常检测
        GcWarning warnings = detectAnomalies(collectors, uptimeMs, totalGcTimeMs);

        return new GcDetailResult(collectors, warnings, System.currentTimeMillis());
    }

    @Override
    public ThreadDump getThreadDump() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        ThreadInfo[] allThreads = threadBean.dumpAllThreads(true, true, STACK_DEPTH);

        List<ThreadItem> threads = new ArrayList<>();
        int virtual = 0, platform = 0;

        for (ThreadInfo ti : allThreads) {
            boolean isVirtual = isVirtualThread(ti);
            if (isVirtual) virtual++; else platform++;

            threads.add(new ThreadItem(
                    ti.getThreadId(),
                    ti.getThreadName(),
                    ti.getThreadState().name(),
                    isVirtual,
                    0L,
                    formatStackTrace(ti.getStackTrace())
            ));
        }

        return new ThreadDump(allThreads.length, virtual, platform, threads);
    }

    // ==================== GC 异常检测 ====================

    private GcWarning detectAnomalies(List<GcCollectorInfo> collectors, long uptimeMs, long totalGcTimeMs) {
        List<String> warnings = new ArrayList<>();
        String severity = "NORMAL";

        double uptimeHours = Math.max(uptimeMs / 3_600_000.0, 0.01);
        double gcOverheadPercent = (double) totalGcTimeMs / uptimeMs * 100;

        // JVM 启动不足 5 分钟时，不触发频率告警（启动阶段 GC 自然频繁）
        boolean uptimeGated = uptimeMs < MIN_UPTIME_MS_FOR_GC_CHECK;

        for (GcCollectorInfo gc : collectors) {
            String name = gc.name().toLowerCase();
            boolean isYoung = name.contains("young") || name.contains("copy")
                    || name.contains("scavenge") || name.contains("parnew")
                    || name.contains("eden") || name.contains("survivor");
            // G1 Concurrent GC / ZGC Concurrent 是并发标记，不是 STW Full GC，不计入 Full GC 告警
            boolean isConcurrent = name.contains("concurrent") || name.contains("zgc")
                    || name.contains("shenandoah");
            boolean isOld = !isConcurrent && (name.contains("old") || name.contains("full")
                    || name.contains("mark") || name.contains("mixed")
                    || name.contains("compact"));

            // 1. 检测频繁 GC（需满足最小运行时间）
            if (!uptimeGated && isYoung && gc.collectionsPerHour() > YOUNG_GC_PER_HOUR_WARN) {
                warnings.add("Young GC 频繁: %s 收集器 %.0f 次/小时（阈值 >%.0f），已收集 %d 次"
                        .formatted(gc.name(), gc.collectionsPerHour(), YOUNG_GC_PER_HOUR_WARN, gc.collectionCount()));
            }
            if (!uptimeGated && isOld && gc.collectionsPerHour() > FULL_GC_PER_HOUR_WARN) {
                if (gc.collectionsPerHour() > FULL_GC_PER_HOUR_SEVERE) {
                    warnings.add("Full GC 严重频繁: %s 收集器 %.1f 次/小时（阈值 >%.0f），已收集 %d 次，可能存在内存泄漏"
                            .formatted(gc.name(), gc.collectionsPerHour(), FULL_GC_PER_HOUR_SEVERE, gc.collectionCount()));
                    severity = "SEVERE";
                } else {
                    warnings.add("Full GC 较频繁: %s 收集器 %.1f 次/小时（阈值 >%.0f），已收集 %d 次，建议检查堆内存配置"
                            .formatted(gc.name(), gc.collectionsPerHour(), FULL_GC_PER_HOUR_WARN, gc.collectionCount()));
                    if (!"SEVERE".equals(severity)) severity = "WARN";
                }
            }

            // 2. 检测 GC 暂停时间过长
            if (isYoung && gc.avgTimeMs() > YOUNG_GC_PAUSE_MS_WARN) {
                warnings.add("Young GC 平均暂停时间长: %s 平均 %.1f ms/次（阈值 >%d ms）"
                        .formatted(gc.name(), gc.avgTimeMs(), YOUNG_GC_PAUSE_MS_WARN));
            }
            if (isOld && gc.avgTimeMs() > FULL_GC_PAUSE_MS_WARN) {
                warnings.add("Full GC 平均暂停时间长: %s 平均 %.1f ms/次（阈值 >%d ms），可能导致请求超时"
                        .formatted(gc.name(), gc.avgTimeMs(), FULL_GC_PAUSE_MS_WARN));
            }

            // 3. Young GC 平均耗时偏高（虽未超过500ms但超过200ms）
            if (isYoung && gc.avgTimeMs() > YOUNG_GC_AVG_MS_WARN && gc.avgTimeMs() <= YOUNG_GC_PAUSE_MS_WARN) {
                warnings.add("Young GC 耗时偏高: %s 平均 %.1f ms/次（阈值 >%d ms），可能堆内存不足或对象分配过快"
                        .formatted(gc.name(), gc.avgTimeMs(), YOUNG_GC_AVG_MS_WARN));
            }

            // 4. 检测上次 GC 后内存效果不佳（Full GC 没回收多少）
            if (isOld && gc.lastGc() != null && gc.lastGc().pools() != null) {
                long totalFreed = gc.lastGc().pools().values().stream()
                        .mapToLong(MemoryPoolDelta::freedBytes).sum();
                // 找出老年代/堆内存池
                for (var entry : gc.lastGc().pools().entrySet()) {
                    String poolName = entry.getKey().toLowerCase();
                    MemoryPoolDelta delta = entry.getValue();
                    boolean isOldPool = poolName.contains("old") || poolName.contains("tenured")
                            || poolName.contains("g1 old");
                    if (isOldPool && delta.usageAfterPercent() > OLD_GEN_USAGE_AFTER_GC_WARN) {
                        warnings.add("Full GC 效果不佳: %s 回收后 %s 使用率仍达 %.1f%%（已用 %s/%s），总释放 %s，建议增大堆内存"
                                .formatted(gc.name(), entry.getKey(), delta.usageAfterPercent(),
                                        formatBytes(delta.usedAfter()), formatBytes(delta.max()),
                                        formatBytes(totalFreed)));
                        break;
                    }
                }
            }

            // 5. 长时间未发生 GC（可能 JVM 已僵死或 GC 线程异常）
            if (gc.lastGc() != null && gc.lastGc().elapsedSinceMs() > ELAPSED_SINCE_GC_MS_WARN && gc.collectionCount() > 0) {
                warnings.add("GC 长时间未执行: %s 距上次 GC 已过 %s（阈值 >%s），请检查 GC 线程是否正常"
                        .formatted(gc.name(), formatDuration(gc.lastGc().elapsedSinceMs()),
                                formatDuration(ELAPSED_SINCE_GC_MS_WARN)));
            }
        }

        // 6. 全局 GC 开销过高
        if (gcOverheadPercent > GC_OVERHEAD_SEVERE) {
            warnings.add("GC 总开销严重: GC 总耗时占 JVM 运行时间的 %.1f%%（阈值 >%.0f%%），CPU 大部分时间被 GC 占用"
                    .formatted(gcOverheadPercent, GC_OVERHEAD_SEVERE));
            if (!"SEVERE".equals(severity)) severity = "SEVERE";
        } else if (gcOverheadPercent > GC_OVERHEAD_WARN) {
            warnings.add("GC 总开销偏高: GC 总耗时占 JVM 运行时间的 %.1f%%（阈值 >%.0f%%），建议优化 GC 策略或增大堆内存"
                    .formatted(gcOverheadPercent, GC_OVERHEAD_WARN));
            if (!"SEVERE".equals(severity)) severity = "WARN";
        }

        // 7. 没有收集器数据
        if (collectors.isEmpty()) {
            warnings.add("未检测到任何 GC 收集器，GC 监控数据不可用");
        }

        boolean hasWarning = !warnings.isEmpty();
        if (!hasWarning) {
            severity = "NORMAL";
        }

        return new GcWarning(hasWarning, severity, warnings);
    }

    // ==================== 内部构建方法 ====================

    private MemorySummary buildMemorySummary() {
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memory.getHeapMemoryUsage();
        MemoryUsage nonHeap = memory.getNonHeapMemoryUsage();

        long metaspaceUsed = 0, metaspaceMax = 0;
        List<MemoryPoolInfo> pools = new ArrayList<>();

        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            MemoryUsage usage = pool.getUsage();
            if (usage != null) {
                String name = pool.getName();
                long max = usage.getMax() > 0 ? usage.getMax() : usage.getCommitted();
                pools.add(new MemoryPoolInfo(name, usage.getUsed(), usage.getCommitted(), max,
                        max > 0 ? (double) usage.getUsed() / max * 100 : 0));

                if ("Metaspace".equals(name)) {
                    metaspaceUsed = usage.getUsed();
                    metaspaceMax = max;
                }
            }
        }

        return new MemorySummary(
                heap.getUsed(), heap.getMax(), heap.getCommitted(),
                heap.getMax() > 0 ? (double) heap.getUsed() / heap.getMax() * 100 : 0,
                nonHeap.getUsed(), nonHeap.getCommitted(),
                metaspaceUsed, metaspaceMax,
                pools
        );
    }

    private ThreadSummary buildThreadSummary(ThreadMXBean bean) {
        // 区分虚拟线程和平台线程
        ThreadInfo[] all = bean.dumpAllThreads(false, false, 0);
        int virtual = 0;
        for (ThreadInfo ti : all) {
            if (isVirtualThread(ti)) virtual++;
        }
        int platform = all.length - virtual;

        return new ThreadSummary(
                bean.getThreadCount(),
                virtual,
                platform,
                bean.getDaemonThreadCount(),
                bean.getPeakThreadCount(),
                bean.getTotalStartedThreadCount()
        );
    }

    private boolean isVirtualThread(ThreadInfo ti) {
        if (ti == null) return false;
        String name = ti.getThreadName();
        return name != null && name.startsWith("VirtualThread[");
    }

    private String formatStackTrace(StackTraceElement[] stack) {
        if (stack == null || stack.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(stack.length, 15);
        for (int i = 0; i < limit; i++) {
            if (i > 0) sb.append("\n");
            sb.append("    at ").append(stack[i].toString());
        }
        if (stack.length > limit) {
            sb.append("\n    ... ").append(stack.length - limit).append(" more");
        }
        return sb.toString();
    }

    private String formatUptime(long ms) {
        long days = ms / 86_400_000;
        long hours = (ms % 86_400_000) / 3_600_000;
        long minutes = (ms % 3_600_000) / 60_000;
        long seconds = (ms % 60_000) / 1_000;

        if (days > 0) return String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
        if (hours > 0) return String.format("%dh %dm %ds", hours, minutes, seconds);
        if (minutes > 0) return String.format("%dm %ds", minutes, seconds);
        return String.format("%ds", seconds);
    }

    private String formatDuration(long ms) {
        if (ms < 60_000) return String.format("%.1f 秒", ms / 1000.0);
        if (ms < 3_600_000) return String.format("%.1f 分钟", ms / 60_000.0);
        return String.format("%.1f 小时", ms / 3_600_000.0);
    }

    private String formatBytes(long bytes) {
        if (bytes < 0) return "无限制";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1_048_576) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1_073_741_824) return String.format("%.1f MB", bytes / 1_048_576.0);
        return String.format("%.2f GB", bytes / 1_073_741_824.0);
    }
}
