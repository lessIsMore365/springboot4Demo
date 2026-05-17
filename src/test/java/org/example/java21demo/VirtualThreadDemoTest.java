package org.example.java21demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("虚拟线程演示测试")
class VirtualThreadDemoTest {

    private final VirtualThreadDemoService service = new VirtualThreadDemoService();

    // ==================== 基础 API 测试 ====================

    @Test
    @DisplayName("Thread.ofVirtual() 创建虚拟线程并执行")
    void testCreateVirtualThread() throws InterruptedException {
        AtomicInteger result = new AtomicInteger(0);

        Thread vt = Thread.ofVirtual()
                .name("test-vt")
                .start(() -> result.set(42));

        vt.join();
        assertEquals(42, result.get());
        assertTrue(vt.isVirtual(), "Thread.ofVirtual() 创建的应是虚拟线程");
    }

    @Test
    @DisplayName("Thread.ofPlatform() 创建平台线程并执行")
    void testCreatePlatformThread() throws InterruptedException {
        AtomicInteger result = new AtomicInteger(0);

        Thread pt = Thread.ofPlatform()
                .name("test-pt")
                .start(() -> result.set(99));

        pt.join();
        assertEquals(99, result.get());
        assertFalse(pt.isVirtual(), "Thread.ofPlatform() 创建的应是平台线程");
    }

    @Test
    @DisplayName("当前主线程应为平台线程")
    void testMainThreadIsPlatform() {
        Thread current = Thread.currentThread();
        assertFalse(current.isVirtual(), "主线程应为平台线程");
    }

    // ==================== 批量创建测试 ====================

    @Test
    @DisplayName("批量创建虚拟线程应在极短时间内完成")
    void testBatchVirtualThreadCreation() {
        Map<String, Object> result = service.createVirtualThreads(1000);

        assertEquals(1000, result.get("actualCompleted"));
        long elapsedMs = (long) result.get("elapsedMs");
        assertTrue(elapsedMs < 5000, "1000 个虚拟线程应在 5 秒内创建完成，实际: " + elapsedMs + "ms");
    }

    @Test
    @DisplayName("批量创建平台线程")
    void testBatchPlatformThreadCreation() {
        Map<String, Object> result = service.createPlatformThreads(100);

        assertEquals(100, result.get("actualCompleted"));
        assertEquals("PLATFORM", result.get("threadType"));
    }

    @Test
    @DisplayName("虚拟线程创建速度应远快于平台线程")
    void testVirtualFasterThanPlatform() {
        Map<String, Object> vResult = service.createVirtualThreads(1000);
        Map<String, Object> pResult = service.createPlatformThreads(100);

        long vMs = (long) vResult.get("elapsedMs");
        long pMs = (long) pResult.get("elapsedMs");

        double vThroughput = (long) vResult.get("throughputPerSecond");
        double pThroughput = 100 * 1000L / Math.max((long) pMs, 1);

        assertTrue(vThroughput > pThroughput,
                String.format("虚拟线程吞吐量(%.0f/s)应大于平台线程(%.0f/s)", vThroughput, pThroughput));
    }

    // ==================== Executor API 测试 ====================

    @Test
    @DisplayName("Executors.newVirtualThreadPerTaskExecutor() 应执行在虚拟线程上")
    void testVirtualThreadExecutor() {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Thread[] capturedThread = new Thread[1];
            executor.submit(() -> {
                capturedThread[0] = Thread.currentThread();
            });

            // 等待任务完成
            try { Thread.sleep(50); } catch (InterruptedException e) { /* ignore */ }

            assertNotNull(capturedThread[0], "任务应已执行");
            assertTrue(capturedThread[0].isVirtual(),
                    "newVirtualThreadPerTaskExecutor 提交的任务应在虚拟线程上执行，实际: "
                            + capturedThread[0]);
        }
    }

    // ==================== 线程信息测试 ====================

    @Test
    @DisplayName("threadInfo 应返回正确的线程信息字段")
    void testThreadInfo() {
        Map<String, Object> info = service.threadInfo();

        assertNotNull(info.get("threadName"));
        assertNotNull(info.get("threadId"));
        assertNotNull(info.get("isVirtual"));
        assertNotNull(info.get("isDaemon"));
    }

    // ==================== 海量虚拟线程测试 ====================

    @Test
    @DisplayName("创建 10000 个虚拟线程任务应在合理时间内完成")
    void testMassiveVirtualThreads() {
        assertTimeout(Duration.ofSeconds(10), () -> {
            Map<String, Object> result = service.massiveVirtualThreads(10_000);
            assertEquals(10_000, result.get("completedTasks"));
        }, "10000 个虚拟线程任务应在 10 秒内完成");
    }

    // ==================== CountDownLatch 协调测试 ====================

    @Test
    @DisplayName("虚拟线程与 CountDownLatch 配合使用")
    void testCountDownLatchWithVirtualThreads() throws InterruptedException {
        int taskCount = 100;
        CountDownLatch latch = new CountDownLatch(taskCount);
        AtomicInteger counter = new AtomicInteger(0);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < taskCount; i++) {
                executor.submit(() -> {
                    counter.incrementAndGet();
                    latch.countDown();
                });
            }
        }

        assertTrue(latch.await(5, java.util.concurrent.TimeUnit.SECONDS),
                "所有任务应在 5 秒内完成");
        assertEquals(taskCount, counter.get());
    }

    // ==================== Pinning 检测测试 ====================

    @Test
    @DisplayName("线程 pinning 检测应返回合理的结果")
    void testPinningDetection() {
        Map<String, Object> result = service.detectPinning();
        assertNotNull(result.get("elapsedMs"));
        assertNotNull(result.get("note"));
    }

    // ==================== Builder API 测试 ====================

    @Test
    @DisplayName("Thread.ofVirtual 链式 API")
    void testThreadBuilderApi() {
        Map<String, Object> result = service.threadBuilderApi();
        assertEquals("builder-demo", result.get("builderName"));
    }

    @Test
    @DisplayName("传统线程池 vs 虚拟线程对比")
    void testCompareThreadPoolVsVirtual() {
        Map<String, Object> result = service.compareThreadPoolVsVirtual(50, 10);

        Map<String, Object> poolResult = (Map<String, Object>) result.get("threadPool");
        Map<String, Object> vtResult = (Map<String, Object>) result.get("virtualThread");

        assertTrue((long) poolResult.get("elapsedMs") >= 0);
        assertTrue((long) vtResult.get("elapsedMs") >= 0);
        assertTrue(result.get("speedup").toString().contains("x"));
    }
}
