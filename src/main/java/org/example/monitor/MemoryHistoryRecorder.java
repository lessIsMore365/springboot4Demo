package org.example.monitor;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 堆内存历史采样器，按固定间隔采集内存快照，用于生成实时变化曲线图。
 */
@Slf4j
@Component
public class MemoryHistoryRecorder {

    private static final int MAX_SAMPLES = 360; // 30 分钟 (5s 间隔)

    private final ConcurrentLinkedDeque<MemorySample> samples = new ConcurrentLinkedDeque<>();

    @Getter
    private volatile int sampleIntervalSeconds = 5;

    @PostConstruct
    void init() {
        // 启动时立即采集第一个样本
        recordSample();
        log.info("堆内存历史采样器启动 — 间隔 {}s, 最大样本数 {}", sampleIntervalSeconds, MAX_SAMPLES);
    }

    @Scheduled(fixedDelayString = "${monitor.memory.sample-interval-ms:5000}")
    public void recordSample() {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heap = memoryBean.getHeapMemoryUsage();
            MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();

            MemorySample sample = new MemorySample(
                    System.currentTimeMillis(),
                    heap.getUsed(),
                    heap.getMax() > 0 ? heap.getMax() : heap.getCommitted(),
                    heap.getCommitted(),
                    heap.getUsed() * 100.0 / (heap.getMax() > 0 ? heap.getMax() : heap.getCommitted()),
                    nonHeap.getUsed()
            );

            samples.addLast(sample);
            while (samples.size() > MAX_SAMPLES) {
                samples.pollFirst();
            }
        } catch (Exception e) {
            log.debug("采集内存样本失败: {}", e.getMessage());
        }
    }

    /**
     * 获取最近 N 秒的样本
     */
    public List<MemorySample> getHistory(int seconds) {
        long cutoff = System.currentTimeMillis() - (long) seconds * 1000;
        List<MemorySample> result = new ArrayList<>();
        for (MemorySample s : samples) {
            if (s.timestamp >= cutoff) {
                result.add(s);
            }
        }
        return result;
    }

    /**
     * 获取最近 N 个样本
     */
    public List<MemorySample> getRecentSamples(int count) {
        List<MemorySample> result = new ArrayList<>(samples);
        if (result.size() <= count) {
            return result;
        }
        return result.subList(result.size() - count, result.size());
    }

    /** 总样本数 */
    public int getSampleCount() {
        return samples.size();
    }

    /** 内存采样数据点 */
    public record MemorySample(
            long timestamp,
            long heapUsed,
            long heapMax,
            long heapCommitted,
            double heapUsagePercent,
            long nonHeapUsed
    ) {}
}
