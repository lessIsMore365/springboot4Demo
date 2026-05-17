package org.example.config;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.ResultHandler;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MyBatis 慢 SQL 拦截器
 * 拦截 StatementHandler 的 query/update/batch 方法，测量执行时间，
 * 记录超过阈值的慢 SQL 并聚合统计。
 */
@Intercepts({
        @Signature(type = StatementHandler.class, method = "query", args = {Statement.class, ResultHandler.class}),
        @Signature(type = StatementHandler.class, method = "update", args = {Statement.class}),
        @Signature(type = StatementHandler.class, method = "batch", args = {Statement.class})
})
public class SlowSqlInterceptor implements Interceptor {

    /** 慢 SQL 阈值（毫秒），默认 1000ms */
    private final AtomicLong thresholdMs = new AtomicLong(1_000);

    /** 聚合统计：SQL 模板 -> 统计信息 */
    private final Map<String, StatEntry> stats = new ConcurrentHashMap<>();

    /** 最近慢 SQL 明细，最多保留 200 条 */
    private final ConcurrentLinkedDeque<DetailEntry> recentSlowSqls = new ConcurrentLinkedDeque<>();
    private static final int MAX_DETAILS = 200;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler handler = (StatementHandler) invocation.getTarget();
        MetaObject metaObject = SystemMetaObject.forObject(handler);

        String sql = getSql(metaObject);
        long start = System.nanoTime();
        try {
            return invocation.proceed();
        } finally {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            record(sql, elapsedMs);
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    private void record(String sql, long elapsedMs) {
        long threshold = thresholdMs.get();

        // 更新聚合统计（所有 SQL 都统计）
        stats.compute(sql, (k, v) -> {
            if (v == null) {
                return new StatEntry(1, elapsedMs, elapsedMs, elapsedMs, elapsedMs,
                        elapsedMs >= threshold ? 1 : 0,
                        elapsedMs >= threshold ? elapsedMs : 0);
            }
            return new StatEntry(
                    v.count + 1,
                    v.totalTimeMs + elapsedMs,
                    Math.max(v.maxTimeMs, elapsedMs),
                    Math.min(v.minTimeMs, elapsedMs),
                    v.lastTimeMs > 0 ? v.lastTimeMs : elapsedMs,
                    v.slowCount + (elapsedMs >= threshold ? 1 : 0),
                    elapsedMs >= threshold ? elapsedMs : v.lastSlowTimeMs
            );
        });

        // 慢 SQL 记录明细
        if (elapsedMs >= threshold) {
            DetailEntry detail = new DetailEntry(sql, elapsedMs, System.currentTimeMillis());
            recentSlowSqls.addFirst(detail);
            while (recentSlowSqls.size() > MAX_DETAILS) {
                recentSlowSqls.pollLast();
            }
        }
    }

    private String getSql(MetaObject metaObject) {
        try {
            // RoutingStatementHandler -> delegate (PreparedStatementHandler) -> boundSql.sql
            Object delegate = metaObject.getValue("delegate");
            if (delegate != null) {
                MetaObject delegateMeta = SystemMetaObject.forObject(delegate);
                Object boundSql = delegateMeta.getValue("boundSql");
                if (boundSql != null) {
                    MetaObject boundSqlMeta = SystemMetaObject.forObject(boundSql);
                    Object sql = boundSqlMeta.getValue("sql");
                    if (sql != null) {
                        return sql.toString().replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ");
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "Unknown SQL";
    }

    // ==================== 公开 API（供监控服务调用） ====================

    public List<SlowSqlStat> getStats() {
        List<SlowSqlStat> result = new ArrayList<>();
        for (var entry : stats.entrySet()) {
            StatEntry e = entry.getValue();
            result.add(new SlowSqlStat(
                    entry.getKey(),
                    e.count,
                    e.totalTimeMs,
                    e.count > 0 ? (double) e.totalTimeMs / e.count : 0,
                    e.maxTimeMs,
                    e.minTimeMs,
                    e.slowCount,
                    e.lastSlowTimeMs
            ));
        }
        result.sort(Comparator.comparingLong(SlowSqlStat::totalTimeMs).reversed());
        return result;
    }

    public List<SlowSqlDetail> getRecentSlowSqls() {
        List<SlowSqlDetail> result = new ArrayList<>();
        for (DetailEntry e : recentSlowSqls) {
            result.add(new SlowSqlDetail(e.sql, e.elapsedMs, e.timestamp));
        }
        return result;
    }

    public void reset() {
        stats.clear();
        recentSlowSqls.clear();
    }

    public long getThresholdMs() {
        return thresholdMs.get();
    }

    public void setThresholdMs(long ms) {
        if (ms > 0) thresholdMs.set(ms);
    }

    // ==================== 内部数据类 ====================

    private record StatEntry(
            long count,
            long totalTimeMs,
            long maxTimeMs,
            long minTimeMs,
            long lastTimeMs,
            long slowCount,
            long lastSlowTimeMs
    ) {}

    private record DetailEntry(
            String sql,
            long elapsedMs,
            long timestamp
    ) {}

    public record SlowSqlStat(
            String sql,
            long count,
            long totalTimeMs,
            double avgTimeMs,
            long maxTimeMs,
            long minTimeMs,
            long slowCount,
            long lastSlowTimeMs
    ) {}

    public record SlowSqlDetail(
            String sql,
            long elapsedMs,
            long timestamp
    ) {}
}
