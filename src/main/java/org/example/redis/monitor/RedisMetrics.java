package org.example.redis.monitor;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存命中率统计 — 内存计数器 + 定时刷入 Redis。
 */
public class RedisMetrics {

    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();
    private final AtomicLong putCount = new AtomicLong();
    private final AtomicLong evictCount = new AtomicLong();

    public void recordHit() { hits.incrementAndGet(); }
    public void recordMiss() { misses.incrementAndGet(); }
    public void recordPut() { putCount.incrementAndGet(); }
    public void recordPut(int n) { putCount.addAndGet(n); }
    public void recordEvict() { evictCount.incrementAndGet(); }
    public void recordEvict(int n) { evictCount.addAndGet(n); }

    public long hits() { return hits.get(); }
    public long misses() { return misses.get(); }
    public long putCount() { return putCount.get(); }
    public long evictCount() { return evictCount.get(); }
    public long total() { return hits.get() + misses.get(); }

    public double hitRate() {
        long total = total();
        return total == 0 ? 0.0 : (double) hits.get() / total;
    }

    public void reset() {
        hits.set(0);
        misses.set(0);
        putCount.set(0);
        evictCount.set(0);
    }
}
