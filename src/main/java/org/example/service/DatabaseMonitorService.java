package org.example.service;

import java.util.List;

/**
 * 数据库监控服务接口
 * 提供连接池状态、数据库元信息、表统计等实时监控数据
 */
public interface DatabaseMonitorService {

    /** 数据库综合概览 */
    DatabaseOverview getOverview();

    /** 连接池详情 */
    ConnectionPoolDetail getConnectionPoolDetail();

    /** 表统计信息 */
    List<TableStat> getTableStats();

    /** 连接延迟测试 */
    ConnectionLatency testConnectionLatency();

    /** 获取慢 SQL 统计 */
    SlowSqlSummary getSlowSqlStats();

    /** 重置慢 SQL 统计 */
    void resetSlowSqlStats();

    // ==================== 数据模型 ====================

    record DatabaseOverview(
            DbMeta db,
            ConnectionPoolInfo pool,
            String health,
            long timestamp
    ) {}

    record DbMeta(
            String productName,
            String productVersion,
            String driverName,
            String driverVersion,
            String jdbcUrl,
            String username,
            String defaultTransactionIsolation,
            boolean supportsBatchUpdates,
            boolean supportsSavepoints,
            boolean supportsStoredProcedures,
            int maxConnections,
            String catalog,
            String schema
    ) {}

    record ConnectionPoolInfo(
            String poolName,
            int activeConnections,
            int idleConnections,
            int totalConnections,
            int threadsAwaitingConnection,
            int maxPoolSize,
            int minIdle,
            double usagePercent,
            long connectionTimeoutMs,
            long idleTimeoutMs,
            long maxLifetimeMs,
            long keepaliveTimeMs
    ) {}

    record ConnectionPoolDetail(
            ConnectionPoolInfo pool,
            CumulativeStats cumulative,
            long uptimeMs,
            String uptimeFormatted
    ) {}

    record CumulativeStats(
            long totalConnectionsCreated,
            long totalConnectionsClosed,
            long totalConnectionTimeouts,
            long totalFailedValidations
    ) {}

    record TableStat(
            String schemaName,
            String tableName,
            String tableType,
            long rowCountEstimate,
            String totalSize,
            String tableSize,
            String indexSize,
            int indexCount,
            long seqScans,
            long idxScans,
            long nTupIns,
            long nTupUpd,
            long nTupDel,
            long nLiveTup,
            long nDeadTup,
            String lastVacuum,
            String lastAnalyze
    ) {}

    record ConnectionLatency(
            long latencyMs,
            boolean valid,
            int timeoutMs,
            String result
    ) {}

    record SlowSqlStat(
            String sql,
            long count,
            long totalTimeMs,
            double avgTimeMs,
            long maxTimeMs,
            long minTimeMs,
            long slowCount,
            long lastSlowTimeMs
    ) {}

    record SlowSqlDetail(
            String sql,
            long elapsedMs,
            long timestamp
    ) {}

    record SlowSqlSummary(
            List<SlowSqlStat> stats,
            List<SlowSqlDetail> recentSlowSqls,
            long thresholdMs,
            long totalSlowCount
    ) {}
}
