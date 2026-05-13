package org.example.service.impl;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.extern.slf4j.Slf4j;
import org.example.service.DatabaseMonitorService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据库监控服务实现
 */
@Slf4j
@Service
public class DatabaseMonitorServiceImpl implements DatabaseMonitorService {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public DatabaseMonitorServiceImpl(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public DatabaseOverview getOverview() {
        DbMeta meta = null;
        try (Connection conn = dataSource.getConnection()) {
            meta = buildDbMeta(conn);
        } catch (Exception e) {
            log.warn("Failed to get DB metadata: {}", e.getMessage());
        }

        ConnectionPoolInfo pool = buildPoolInfo();
        String health = checkHealth();

        return new DatabaseOverview(meta, pool, health, System.currentTimeMillis());
    }

    @Override
    public ConnectionPoolDetail getConnectionPoolDetail() {
        long uptimeMs = 0;
        try {
            if (dataSource instanceof HikariDataSource hds) {
                HikariPoolMXBean pool = hds.getHikariPoolMXBean();
                if (pool != null) {
                    // HikariCP 未直接暴露 uptime，用连接池持续建连推断
                }
            }
        } catch (Exception ignored) {
        }

        return new ConnectionPoolDetail(
                buildPoolInfo(),
                buildCumulativeStats(),
                uptimeMs,
                formatUptime(uptimeMs)
        );
    }

    @Override
    public List<TableStat> getTableStats() {
        List<TableStat> stats = new ArrayList<>();

        try {
            // PostgreSQL pg_stat_user_tables 视图
            String sql = """
                SELECT
                    n.nspname AS schema_name,
                    c.relname AS table_name,
                    CASE c.relkind
                        WHEN 'r' THEN 'TABLE'
                        WHEN 'p' THEN 'PARTITIONED TABLE'
                        WHEN 'v' THEN 'VIEW'
                        ELSE c.relkind::text
                    END AS table_type,
                    COALESCE(s.n_live_tup, 0) AS n_live_tup,
                    COALESCE(s.n_dead_tup, 0) AS n_dead_tup,
                    COALESCE(s.n_tup_ins, 0) AS n_tup_ins,
                    COALESCE(s.n_tup_upd, 0) AS n_tup_upd,
                    COALESCE(s.n_tup_del, 0) AS n_tup_del,
                    COALESCE(s.seq_scan, 0) AS seq_scans,
                    COALESCE(s.idx_scan, 0) AS idx_scans,
                    pg_size_pretty(pg_total_relation_size(c.oid)) AS total_size,
                    pg_size_pretty(pg_table_size(c.oid)) AS table_size,
                    pg_size_pretty(pg_indexes_size(c.oid)) AS index_size,
                    (SELECT count(*) FROM pg_index i WHERE i.indrelid = c.oid) AS index_count,
                    CASE WHEN s.last_vacuum IS NOT NULL
                         THEN s.last_vacuum::text ELSE '-' END AS last_vacuum,
                    CASE WHEN s.last_analyze IS NOT NULL
                         THEN s.last_analyze::text ELSE '-' END AS last_analyze
                FROM pg_class c
                JOIN pg_namespace n ON n.oid = c.relnamespace
                LEFT JOIN pg_stat_user_tables s ON s.relid = c.oid
                WHERE c.relkind IN ('r', 'p')
                  AND n.nspname NOT IN ('pg_catalog', 'information_schema', 'pg_toast')
                ORDER BY pg_total_relation_size(c.oid) DESC
                """;

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            for (Map<String, Object> row : rows) {
                stats.add(new TableStat(
                        str(row, "schema_name"),
                        str(row, "table_name"),
                        str(row, "table_type"),
                        longVal(row, "n_live_tup"),
                        str(row, "total_size"),
                        str(row, "table_size"),
                        str(row, "index_size"),
                        intVal(row, "index_count"),
                        longVal(row, "seq_scans"),
                        longVal(row, "idx_scans"),
                        longVal(row, "n_tup_ins"),
                        longVal(row, "n_tup_upd"),
                        longVal(row, "n_tup_del"),
                        longVal(row, "n_live_tup"),
                        longVal(row, "n_dead_tup"),
                        str(row, "last_vacuum"),
                        str(row, "last_analyze")
                ));
            }
        } catch (Exception e) {
            log.warn("Failed to query table stats: {}", e.getMessage());
        }

        return stats;
    }

    @Override
    public ConnectionLatency testConnectionLatency() {
        boolean valid = false;
        long latencyMs;
        int timeoutMs = 5;

        long start = System.nanoTime();
        try (Connection conn = dataSource.getConnection()) {
            valid = conn.isValid(timeoutMs);
            latencyMs = (System.nanoTime() - start) / 1_000_000;
        } catch (Exception e) {
            latencyMs = (System.nanoTime() - start) / 1_000_000;
            log.warn("Connection latency test failed: {}", e.getMessage());
        }

        String result = valid ? "OK (" + latencyMs + "ms)" : "FAILED (" + latencyMs + "ms)";

        return new ConnectionLatency(latencyMs, valid, timeoutMs, result);
    }

    // ==================== 内部构建方法 ====================

    private DbMeta buildDbMeta(Connection conn) throws Exception {
        DatabaseMetaData meta = conn.getMetaData();

        String isolation = switch (meta.getDefaultTransactionIsolation()) {
            case Connection.TRANSACTION_NONE -> "NONE";
            case Connection.TRANSACTION_READ_UNCOMMITTED -> "READ_UNCOMMITTED";
            case Connection.TRANSACTION_READ_COMMITTED -> "READ_COMMITTED";
            case Connection.TRANSACTION_REPEATABLE_READ -> "REPEATABLE_READ";
            case Connection.TRANSACTION_SERIALIZABLE -> "SERIALIZABLE";
            default -> "UNKNOWN";
        };

        return new DbMeta(
                meta.getDatabaseProductName(),
                meta.getDatabaseProductVersion(),
                meta.getDriverName(),
                meta.getDriverVersion(),
                meta.getURL(),
                meta.getUserName(),
                isolation,
                meta.supportsBatchUpdates(),
                meta.supportsSavepoints(),
                meta.supportsStoredProcedures(),
                meta.getMaxConnections(),
                conn.getCatalog(),
                conn.getSchema()
        );
    }

    private ConnectionPoolInfo buildPoolInfo() {
        int active = -1, idle = -1, total = -1, awaiting = -1,
                maxPool = -1, minIdle = -1;
        long connectionTimeoutMs = -1, idleTimeoutMs = -1,
                maxLifetimeMs = -1, keepaliveTimeMs = -1;
        String poolName = "Unknown";

        try {
            if (dataSource instanceof HikariDataSource hds) {
                poolName = hds.getPoolName();
                maxPool = hds.getMaximumPoolSize();
                minIdle = hds.getMinimumIdle();
                connectionTimeoutMs = hds.getConnectionTimeout();
                idleTimeoutMs = hds.getIdleTimeout();
                maxLifetimeMs = hds.getMaxLifetime();
                keepaliveTimeMs = hds.getKeepaliveTime();

                HikariPoolMXBean pool = hds.getHikariPoolMXBean();
                if (pool != null) {
                    active = pool.getActiveConnections();
                    idle = pool.getIdleConnections();
                    total = pool.getTotalConnections();
                    awaiting = pool.getThreadsAwaitingConnection();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to read HikariCP pool metrics: {}", e.getMessage());
        }

        double usagePercent = maxPool > 0 && active >= 0
                ? (double) active / maxPool * 100 : 0;

        return new ConnectionPoolInfo(
                poolName, active, idle, total, awaiting, maxPool, minIdle,
                usagePercent, connectionTimeoutMs, idleTimeoutMs,
                maxLifetimeMs, keepaliveTimeMs
        );
    }

    private CumulativeStats buildCumulativeStats() {
        long created = -1, closed = -1, timeouts = -1, failedValidations = -1;

        try {
            if (dataSource instanceof HikariDataSource hds) {
                var mbeanServer = java.lang.management.ManagementFactory.getPlatformMBeanServer();
                var poolName = new javax.management.ObjectName(
                        "com.zaxxer.hikari:type=Pool (" + hds.getPoolName() + ")");
                if (mbeanServer.isRegistered(poolName)) {
                    timeouts = getLongAttr(mbeanServer, poolName, "ConnectionTimeoutTotalCount");
                    created = getLongAttr(mbeanServer, poolName, "ConnectionsTotal");
                }
            }
        } catch (Exception ignored) {
            // JMX not available
        }

        return new CumulativeStats(created, closed, timeouts, failedValidations);
    }

    private static long getLongAttr(javax.management.MBeanServerConnection mbeanServer,
                                     javax.management.ObjectName name, String attr) {
        try {
            Object val = mbeanServer.getAttribute(name, attr);
            if (val instanceof Number n) return n.longValue();
        } catch (Exception ignored) {
        }
        return -1;
    }

    private String checkHealth() {
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(5);
            return valid ? "HEALTHY" : "UNHEALTHY";
        } catch (Exception e) {
            return "DOWN: " + e.getMessage();
        }
    }

    private static String str(Map<String, Object> row, String key) {
        Object v = row.get(key);
        return v != null ? v.toString() : "-";
    }

    private static long longVal(Map<String, Object> row, String key) {
        Object v = row.get(key);
        if (v instanceof Number n) return n.longValue();
        return 0;
    }

    private static int intVal(Map<String, Object> row, String key) {
        Object v = row.get(key);
        if (v instanceof Number n) return n.intValue();
        return 0;
    }

    private String formatUptime(long ms) {
        if (ms <= 0) return "N/A";
        long days = ms / 86_400_000;
        long hours = (ms % 86_400_000) / 3_600_000;
        long minutes = (ms % 3_600_000) / 60_000;
        long seconds = (ms % 60_000) / 1_000;

        if (days > 0) return String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
        if (hours > 0) return String.format("%dh %dm %ds", hours, minutes, seconds);
        if (minutes > 0) return String.format("%dm %ds", minutes, seconds);
        return String.format("%ds", seconds);
    }
}
