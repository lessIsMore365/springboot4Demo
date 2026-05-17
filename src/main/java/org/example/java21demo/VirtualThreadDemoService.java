package org.example.java21demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class VirtualThreadDemoService {

    // ==================== 1. 线程信息 ====================

    public Map<String, Object> threadInfo() {
        Thread t = Thread.currentThread();
        return Map.of(
                "threadName", t.getName(),
                "threadId", t.threadId(),
                "isVirtual", t.isVirtual(),
                "isDaemon", t.isDaemon(),
                "priority", t.getPriority(),
                "threadGroup", t.getThreadGroup() != null ? t.getThreadGroup().getName() : "N/A"
        );
    }

    // ==================== 2. 虚拟线程创建 ====================

    public Map<String, Object> createVirtualThreads(int count) {
        Instant start = Instant.now();
        AtomicInteger completed = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(count);

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Thread vt = Thread.ofVirtual()
                    .name("demo-vt-", i)
                    .start(() -> {
                        completed.incrementAndGet();
                        latch.countDown();
                    });
            threads.add(vt);
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long elapsedMs = Duration.between(start, Instant.now()).toMillis();
        log.info("创建 {} 个虚拟线程耗时: {}ms", count, elapsedMs);

        return Map.of(
                "threadType", "VIRTUAL",
                "requestedCount", count,
                "actualCompleted", completed.get(),
                "elapsedMs", elapsedMs,
                "throughputPerSecond", count * 1000L / Math.max(elapsedMs, 1)
        );
    }

    // ==================== 3. 平台线程创建（对比） ====================

    public Map<String, Object> createPlatformThreads(int count) {
        if (count > 500) {
            count = 500;
            log.warn("平台线程数量限制为 500，避免系统资源耗尽");
        }

        Instant start = Instant.now();
        AtomicInteger completed = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(count);

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Thread pt = Thread.ofPlatform()
                    .name("demo-pt-", i)
                    .start(() -> {
                        completed.incrementAndGet();
                        latch.countDown();
                    });
            threads.add(pt);
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long elapsedMs = Duration.between(start, Instant.now()).toMillis();
        log.info("创建 {} 个平台线程耗时: {}ms", count, elapsedMs);

        return Map.of(
                "threadType", "PLATFORM",
                "requestedCount", count,
                "actualCompleted", completed.get(),
                "elapsedMs", elapsedMs,
                "note", "平台线程受 OS 限制，创建大量平台线程可能导致 OOM"
        );
    }

    // ==================== 4. 虚拟线程 vs 平台线程对比 ====================

    public Map<String, Object> compare(int virtualCount, int platformCount) {
        Map<String, Object> virtualResult = createVirtualThreads(virtualCount);
        Map<String, Object> platformResult = createPlatformThreads(platformCount);

        return Map.of(
                "virtualThreadResult", virtualResult,
                "platformThreadResult", platformResult,
                "summary", String.format(
                        "虚拟线程: %d个/%dms, 平台线程: %d个/%dms",
                        virtualCount, virtualResult.get("elapsedMs"),
                        platformCount, platformResult.get("elapsedMs")
                )
        );
    }

    // ==================== 5. 海量虚拟线程 ====================

    public Map<String, Object> massiveVirtualThreads(int count) {
        int actualCount = Math.min(count, 1_000_000);
        Instant start = Instant.now();

        AtomicInteger counter = new AtomicInteger(0);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (int i = 0; i < actualCount; i++) {
                int taskId = i;
                futures.add(CompletableFuture.runAsync(() -> {
                    counter.incrementAndGet();
                    if (taskId % 100_000 == 0 && taskId > 0) {
                        log.info("已完成 {} 个虚拟线程任务", taskId);
                    }
                }, executor));
            }
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        }

        long elapsedMs = Duration.between(start, Instant.now()).toMillis();
        log.info("海量虚拟线程测试: {} 个任务, 耗时 {}ms", actualCount, elapsedMs);

        return Map.of(
                "totalTasks", actualCount,
                "completedTasks", counter.get(),
                "elapsedMs", elapsedMs,
                "avgTaskTimeUs", elapsedMs * 1000L / Math.max(actualCount, 1)
        );
    }

    // ==================== 6. 线程 Pinning 检测 ====================

    public Map<String, Object> detectPinning() {
        Instant start = Instant.now();
        int taskCount = 100;

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CountDownLatch latch = new CountDownLatch(taskCount);
            for (int i = 0; i < taskCount; i++) {
                executor.submit(() -> {
                    // synchronized 可能导致虚拟线程 pinning（卡在平台线程上）
                    synchronized (this) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    latch.countDown();
                });
            }
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long elapsedMs = Duration.between(start, Instant.now()).toMillis();

        return Map.of(
                "taskCount", taskCount,
                "elapsedMs", elapsedMs,
                "note", "synchronized 块内 sleep 会导致虚拟线程 pinning（JDK 24+ 已修复）",
                "pinningEffect", taskCount > 0 ? String.format(
                        "如果每个任务串行执行需要 %dms（%d × 10ms），实际耗时 %dms",
                        taskCount * 10, taskCount, elapsedMs
                ) : "N/A"
        );
    }

    // ==================== 7. 异步虚拟线程 ====================

    @Async("taskExecutor")
    public CompletableFuture<Map<String, Object>> asyncDemo() {
        Thread t = Thread.currentThread();
        log.info("异步方法执行于虚拟线程: {} (isVirtual={})", t.getName(), t.isVirtual());

        return CompletableFuture.completedFuture(Map.of(
                "threadName", t.getName(),
                "isVirtual", t.isVirtual(),
                "message", "该方法由 @Async(\"taskExecutor\") 调度到虚拟线程执行"
        ));
    }

    // ==================== 8. Thread.ofVirtual 链式 API ====================

    public Map<String, Object> threadBuilderApi() {
        List<Map<String, String>> threadDetails = Collections.synchronizedList(new ArrayList<>());

        Thread vt = Thread.ofVirtual()
                .name("builder-demo")
                .inheritInheritableThreadLocals(false)
                .uncaughtExceptionHandler((t, e) ->
                        log.error("虚拟线程 {} 未捕获异常: {}", t.getName(), e.getMessage()))
                .start(() -> {
                    Thread current = Thread.currentThread();
                    threadDetails.add(Map.of(
                            "threadName", current.getName(),
                            "isVirtual", String.valueOf(current.isVirtual()),
                            "threadId", String.valueOf(current.threadId())
                    ));
                });

        try {
            vt.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return Map.of(
                "builderName", "builder-demo",
                "threadDetails", threadDetails,
                "apiUsed", "Thread.ofVirtual().name().inheritInheritableThreadLocals().uncaughtExceptionHandler().start()"
        );
    }

    // ==================== 9. 传统线程池 vs 虚拟线程 ====================

    public Map<String, Object> compareThreadPoolVsVirtual(int taskCount, int sleepMs) {
        int count = Math.min(taskCount, 5000);

        // ---- 传统做法：固定大小线程池 ----
        var pool = java.util.concurrent.Executors.newFixedThreadPool(50);
        Instant poolStart = Instant.now();
        AtomicInteger poolDone = new AtomicInteger(0);
        CountDownLatch poolLatch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            pool.submit(() -> {
                try { Thread.sleep(sleepMs); } catch (InterruptedException e) { /* ignore */ }
                poolDone.incrementAndGet();
                poolLatch.countDown();
            });
        }
        try { poolLatch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        long poolMs = Duration.between(poolStart, Instant.now()).toMillis();
        pool.shutdown();

        // ---- 新特性：虚拟线程 ----
        Instant vtStart = Instant.now();
        AtomicInteger vtDone = new AtomicInteger(0);
        CountDownLatch vtLatch = new CountDownLatch(count);
        for (int i = 0; i < count; i++) {
            Thread.ofVirtual().name("compare-vt-", i).start(() -> {
                try { Thread.sleep(sleepMs); } catch (InterruptedException e) { /* ignore */ }
                vtDone.incrementAndGet();
                vtLatch.countDown();
            });
        }
        try { vtLatch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        long vtMs = Duration.between(vtStart, Instant.now()).toMillis();

        return Map.of(
                "taskCount", count,
                "sleepPerTaskMs", sleepMs,
                "threadPool", Map.of(
                        "approach", "Executors.newFixedThreadPool(50) + submit",
                        "poolSize", 50,
                        "elapsedMs", poolMs,
                        "note", count + " 个任务排队在 50 个线程上，串行等待"
                ),
                "virtualThread", Map.of(
                        "approach", "Thread.ofVirtual().start() — 每个任务一个虚拟线程",
                        "elapsedMs", vtMs,
                        "note", count + " 个虚拟线程并发执行，无排队"
                ),
                "speedup", String.format("%.1fx 加速", (double) poolMs / Math.max(vtMs, 1))
        );
    }
}
