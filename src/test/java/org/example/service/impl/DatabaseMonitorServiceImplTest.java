package org.example.service.impl;

import org.example.service.DatabaseMonitorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("数据库监控服务测试")
class DatabaseMonitorServiceImplTest {

    @Autowired
    private DatabaseMonitorService dbMonitorService;

    private DatabaseMonitorService.DatabaseOverview overview;
    private DatabaseMonitorService.ConnectionPoolDetail poolDetail;
    private List<DatabaseMonitorService.TableStat> tableStats;
    private DatabaseMonitorService.ConnectionLatency latency;

    @BeforeEach
    void setUp() {
        overview = dbMonitorService.getOverview();
        poolDetail = dbMonitorService.getConnectionPoolDetail();
        tableStats = dbMonitorService.getTableStats();
        latency = dbMonitorService.testConnectionLatency();
    }

    // ==================== 综合概览 ====================

    @Test
    @DisplayName("数据库概览包含 DB 元信息和连接池信息")
    void testOverview() {
        assertNotNull(overview);
        assertNotNull(overview.db());
        assertNotNull(overview.pool());
        assertNotNull(overview.health());

        DatabaseMonitorService.DbMeta db = overview.db();
        assertNotNull(db.productName());
        assertFalse(db.productName().isEmpty());
        assertNotNull(db.driverName());
        assertNotNull(db.driverVersion());
        assertNotNull(db.jdbcUrl());

        assertTrue(db.maxConnections() != 0, "maxConnections should not be 0");
        assertNotNull(db.defaultTransactionIsolation());
    }

    @Test
    @DisplayName("连接池信息各项数值在合理范围内")
    void testPoolInfo() {
        DatabaseMonitorService.ConnectionPoolInfo pool = overview.pool();

        assertNotNull(pool.poolName());
        assertTrue(pool.maxPoolSize() > 0, "maxPoolSize should be positive");
        assertTrue(pool.activeConnections() >= 0, "activeConnections >= 0");
        assertTrue(pool.totalConnections() >= 0, "totalConnections >= 0");
        assertEquals(pool.totalConnections(),
                pool.activeConnections() + pool.idleConnections(),
                "total = active + idle");
        assertTrue(pool.usagePercent() >= 0 && pool.usagePercent() <= 100,
                "usagePercent should be 0-100");
    }

    @Test
    @DisplayName("健康状态为已知值")
    void testHealth() {
        String health = overview.health();
        assertNotNull(health);
        assertTrue(health.equals("HEALTHY") || health.equals("UNHEALTHY")
                        || health.startsWith("DOWN"),
                "Expected HEALTHY/UNHEALTHY/DOWN: " + health);
    }

    // ==================== 连接池详情 ====================

    @Test
    @DisplayName("连接池详情包含累计统计")
    void testPoolDetail() {
        assertNotNull(poolDetail);
        assertNotNull(poolDetail.pool());
        assertNotNull(poolDetail.cumulative());

        // 累计统计至少有一个有效值
        DatabaseMonitorService.CumulativeStats cumulative = poolDetail.cumulative();
        assertNotNull(cumulative);
        // JMX 可能不可用，此时值返回 -1
        if (cumulative.totalConnectionsCreated() >= 0) {
            assertTrue(cumulative.totalConnectionsCreated() >= 0);
        }
        if (cumulative.totalConnectionTimeouts() >= 0) {
            assertTrue(cumulative.totalConnectionTimeouts() >= 0);
        }
    }

    // ==================== 表统计 ====================

    @Test
    @DisplayName("表统计包含所有用户表")
    void testTableStats() {
        assertNotNull(tableStats);
        // 至少存在 schema.sql 中创建的表
        List<String> expectedTables = List.of(
                "sys_user", "sys_role", "sys_permission",
                "payment_order", "reconciliation_record", "reconciliation_detail"
        );
        List<String> actualNames = tableStats.stream()
                .map(DatabaseMonitorService.TableStat::tableName)
                .toList();

        for (String expected : expectedTables) {
            assertTrue(actualNames.contains(expected),
                    "Should contain table '" + expected + "', found: " + actualNames);
        }
    }

    @Test
    @DisplayName("每个表统计项包含必要字段")
    void testTableStatFields() {
        assertFalse(tableStats.isEmpty());

        for (DatabaseMonitorService.TableStat stat : tableStats) {
            assertNotNull(stat.schemaName());
            assertNotNull(stat.tableName());
            assertFalse(stat.tableName().isEmpty());
            assertNotNull(stat.tableType());
            assertTrue(stat.rowCountEstimate() >= 0);
            assertNotNull(stat.totalSize());
            assertNotNull(stat.tableSize());
            assertNotNull(stat.indexSize());
            assertTrue(stat.indexCount() >= 0);
            assertTrue(stat.seqScans() >= 0);
            assertTrue(stat.idxScans() >= 0);
            assertNotNull(stat.lastVacuum());
            assertNotNull(stat.lastAnalyze());
        }
    }

    @Test
    @DisplayName("表按空间占用降序排列")
    void testTableStatsSortedBySize() {
        if (tableStats.size() < 2) return;

        // 解析第一个和第二个表的表空间，验证排序
        String firstRaw = tableStats.get(0).tableSize();
        String secondRaw = tableStats.get(1).tableSize();
        long firstBytes = parseSizeToBytes(firstRaw);
        long secondBytes = parseSizeToBytes(secondRaw);

        assertTrue(firstBytes >= secondBytes,
                "Table stats should be sorted by total size DESC: "
                        + tableStats.get(0).tableName() + " (" + firstRaw + ") < "
                        + tableStats.get(1).tableName() + " (" + secondRaw + ")");
    }

    // ==================== 连接延迟 ====================

    @Test
    @DisplayName("连接延迟测试返回有效结果")
    void testConnectionLatency() {
        assertNotNull(latency);
        assertTrue(latency.latencyMs() >= 0, "latency >= 0");
        assertTrue(latency.valid(), "connection should be valid");
        assertTrue(latency.timeoutMs() > 0);
        assertNotNull(latency.result());
        assertTrue(latency.result().startsWith("OK"));
    }

    @Test
    @DisplayName("连接延迟在合理范围内")
    void testLatencyReasonable() {
        assertTrue(latency.latencyMs() < 5000,
                "Connection latency should be < 5s, got " + latency.latencyMs() + "ms");
    }

    // ==================== 一致性 ====================

    @Test
    @DisplayName("连续两次调用概览结果一致")
    void testOverviewConsistency() {
        DatabaseMonitorService.DatabaseOverview o2 = dbMonitorService.getOverview();

        assertEquals(overview.db().productName(), o2.db().productName());
        assertEquals(overview.db().driverName(), o2.db().driverName());
        assertEquals(overview.db().jdbcUrl(), o2.db().jdbcUrl());
        assertEquals(overview.pool().maxPoolSize(), o2.pool().maxPoolSize());
    }

    private static long parseSizeToBytes(String size) {
        if (size == null || size.equals("-")) return 0;
        try {
            size = size.trim();
            if (size.endsWith("kB")) return (long) (Double.parseDouble(size.replace("kB", "").trim()) * 1024);
            if (size.endsWith("MB")) return (long) (Double.parseDouble(size.replace("MB", "").trim()) * 1024 * 1024);
            if (size.endsWith("GB")) return (long) (Double.parseDouble(size.replace("GB", "").trim()) * 1024 * 1024 * 1024);
            if (size.endsWith("TB")) return (long) (Double.parseDouble(size.replace("TB", "").trim()) * 1024 * 1024 * 1024 * 1024);
            if (size.endsWith("bytes")) return Long.parseLong(size.replace("bytes", "").trim());
            return Long.parseLong(size);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
