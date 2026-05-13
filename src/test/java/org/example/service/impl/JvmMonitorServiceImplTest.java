package org.example.service.impl;

import org.example.service.JvmMonitorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JVM 监控服务测试")
class JvmMonitorServiceImplTest {

    private JvmMonitorServiceImpl monitorService;

    @BeforeEach
    void setUp() {
        monitorService = new JvmMonitorServiceImpl();
    }

    // ==================== 综合概览 ====================

    @Test
    @DisplayName("JVM 综合概览包含所有关键字段")
    void testOverview() {
        JvmMonitorService.JvmOverview overview = monitorService.getOverview();

        assertNotNull(overview.jvmName());
        assertFalse(overview.jvmName().isEmpty());
        assertNotNull(overview.jvmVersion());
        assertTrue(overview.uptimeMs() > 0);
        assertNotNull(overview.uptimeFormatted());
        assertFalse(overview.uptimeFormatted().isEmpty());
        assertTrue(overview.availableProcessors() > 0);

        // 内存摘要
        JvmMonitorService.MemorySummary memory = overview.memory();
        assertTrue(memory.heapUsed() > 0);
        assertTrue(memory.heapMax() > 0);
        assertTrue(memory.heapUsagePercent() >= 0);
        assertNotNull(memory.pools());
        assertFalse(memory.pools().isEmpty());

        // 线程摘要
        JvmMonitorService.ThreadSummary threads = overview.threads();
        assertTrue(threads.currentCount() > 0);
        assertTrue(threads.totalStarted() > 0);
        assertEquals(threads.currentCount(),
                threads.virtualCount() + threads.platformCount());

        // GC 信息
        assertNotNull(overview.gc());
        assertFalse(overview.gc().collectors().isEmpty());
    }

    // ==================== 内存详情 ====================

    @Test
    @DisplayName("内存详情包含堆/非堆和内存池信息")
    void testMemoryDetail() {
        JvmMonitorService.MemoryDetail detail = monitorService.getMemoryDetail();

        JvmMonitorService.MemorySummary summary = detail.summary();
        assertTrue(summary.heapUsed() > 0);
        assertTrue(summary.heapMax() > 0);
        assertTrue(summary.heapCommitted() > 0);
        assertTrue(summary.heapUsagePercent() >= 0);
        assertTrue(summary.nonHeapCommitted() > 0);

        // 内存池
        List<JvmMonitorService.MemoryPoolInfo> pools = summary.pools();
        assertFalse(pools.isEmpty());
        for (JvmMonitorService.MemoryPoolInfo pool : pools) {
            assertNotNull(pool.name());
            assertFalse(pool.name().isEmpty());
            assertTrue(pool.committed() >= 0);
            assertTrue(pool.usagePercent() >= 0);
        }

        // 物理内存快照
        assertNotNull(detail.snapshot());
    }

    // ==================== 线程详情 ====================

    @Test
    @DisplayName("线程详情包含虚拟线程/平台线程统计和 CPU Top")
    void testThreadDetail() {
        JvmMonitorService.ThreadDetail detail = monitorService.getThreadDetail();

        JvmMonitorService.ThreadSummary summary = detail.summary();
        assertTrue(summary.currentCount() > 0, "至少有一个线程（当前测试线程）");
        assertEquals(summary.currentCount(),
                summary.virtualCount() + summary.platformCount(),
                "虚拟 + 平台线程数应等于总线程数");
        assertTrue(summary.peakCount() >= summary.currentCount());

        // CPU Top 线程
        assertNotNull(detail.topCpuThreads());
        assertFalse(detail.topCpuThreads().isEmpty());

        // 状态分布
        List<JvmMonitorService.ThreadStateCount> states = detail.stateDistribution();
        assertFalse(states.isEmpty());
        int totalInStates = states.stream().mapToInt(JvmMonitorService.ThreadStateCount::count).sum();
        assertTrue(totalInStates > 0);

        // 必须有 RUNNABLE 状态
        assertTrue(states.stream().anyMatch(s -> "RUNNABLE".equals(s.state())));
    }

    @Test
    @DisplayName("线程详情中虚拟线程和平台线程分类正确")
    void testThreadClassification() {
        JvmMonitorService.ThreadDetail detail = monitorService.getThreadDetail();

        for (JvmMonitorService.ThreadItem item : detail.topCpuThreads()) {
            assertNotNull(item.name());
            assertNotNull(item.state());
            // 虚拟线程名称应以 "VirtualThread[" 开头
            if (item.virtual()) {
                assertTrue(item.name().startsWith("VirtualThread["),
                        "虚拟线程应包含 VirtualThread[ 前缀: " + item.name());
            }
        }
    }

    // ==================== GC 详情 ====================

    @Test
    @DisplayName("GC 详情包含至少一个 GC 收集器及告警信息")
    void testGcDetail() {
        JvmMonitorService.GcDetailResult result = monitorService.getGcDetail();

        assertNotNull(result);
        assertNotNull(result.collectors());
        assertFalse(result.collectors().isEmpty(), "至少有一个 GC 收集器");

        for (JvmMonitorService.GcCollectorInfo gc : result.collectors()) {
            assertNotNull(gc.name());
            assertFalse(gc.name().isEmpty());
            assertTrue(gc.collectionCount() >= 0);
            assertTrue(gc.collectionTimeMs() >= 0);
            assertTrue(gc.avgTimeMs() >= 0);
            assertTrue(gc.collectionsPerHour() >= 0);
        }

        // 告警信息存在
        assertNotNull(result.warnings());
        assertNotNull(result.warnings().severity());
        assertNotNull(result.warnings().warnings());
    }

    // ==================== 线程转储 ====================

    @Test
    @DisplayName("线程转储包含所有线程和堆栈")
    void testThreadDump() {
        JvmMonitorService.ThreadDump dump = monitorService.getThreadDump();

        assertTrue(dump.totalCount() > 0, "至少有一个线程");
        assertEquals(dump.totalCount(),
                dump.virtualCount() + dump.platformCount(),
                "虚拟 + 平台线程数应等于总线程数");

        List<JvmMonitorService.ThreadItem> threads = dump.threads();
        assertFalse(threads.isEmpty());
        assertEquals(dump.totalCount(), threads.size());

        // 验证每个线程项
        for (JvmMonitorService.ThreadItem item : threads) {
            assertTrue(item.id() > 0);
            assertNotNull(item.name());
            assertNotNull(item.state());
            // stackTrace 可为空（部分线程无堆栈）
        }
    }

    @Test
    @DisplayName("线程转储包含当前测试线程")
    void testThreadDumpContainsCurrentThread() {
        JvmMonitorService.ThreadDump dump = monitorService.getThreadDump();
        long currentId = Thread.currentThread().threadId();
        String currentName = Thread.currentThread().getName();

        boolean found = dump.threads().stream()
                .anyMatch(t -> t.id() == currentId && t.name().equals(currentName));
        assertTrue(found, "线程转储应包含当前测试线程");
    }

    // ==================== 边界和一致性 ====================

    @Test
    @DisplayName("连续两次调用概览结果一致")
    void testOverviewConsistency() {
        JvmMonitorService.JvmOverview o1 = monitorService.getOverview();
        JvmMonitorService.JvmOverview o2 = monitorService.getOverview();

        assertEquals(o1.jvmName(), o2.jvmName());
        assertEquals(o1.jvmVersion(), o2.jvmVersion());
        assertEquals(o1.availableProcessors(), o2.availableProcessors());
        assertTrue(o2.uptimeMs() >= o1.uptimeMs(), "运行时间应单调递增");
    }

    @Test
    @DisplayName("内存使用率在合理范围内")
    void testMemoryUsageReasonable() {
        JvmMonitorService.MemoryDetail detail = monitorService.getMemoryDetail();
        JvmMonitorService.MemorySummary summary = detail.summary();

        assertTrue(summary.heapUsagePercent() >= 0 && summary.heapUsagePercent() <= 100,
                "堆使用率应在 0-100% 之间: " + summary.heapUsagePercent());
        assertTrue(summary.heapUsed() <= summary.heapCommitted(),
                "已使用内存不应超过已提交内存");
    }

    @Test
    @DisplayName("峰值线程数不小于当前线程数")
    void testPeakThreadsGteCurrent() {
        JvmMonitorService.ThreadSummary threads = monitorService.getThreadDetail().summary();
        assertTrue(threads.peakCount() >= threads.currentCount(),
                "峰值线程数应 >= 当前线程数");
    }
}
