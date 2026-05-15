package org.example.service.impl;

import org.example.service.ServerMonitorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("服务器监控服务测试")
class ServerMonitorServiceImplTest {

    @Autowired
    private ServerMonitorService serverMonitorService;

    private ServerMonitorService.ServerOverview overview;
    private ServerMonitorService.CpuDetail cpuDetail;
    private ServerMonitorService.MemoryDetail memoryDetail;
    private List<ServerMonitorService.DiskInfo> disks;
    private List<ServerMonitorService.NetworkIfInfo> networks;

    @BeforeEach
    void setUp() {
        overview = serverMonitorService.getOverview();
        cpuDetail = serverMonitorService.getCpuDetail();
        memoryDetail = serverMonitorService.getMemoryDetail();
        disks = serverMonitorService.getDiskDetail();
        networks = serverMonitorService.getNetworkDetail();
    }

    // ==================== 综合概览 ====================

    @Test
    @DisplayName("服务器概览包含所有关键组件")
    void testOverview() {
        assertNotNull(overview);

        // OS
        ServerMonitorService.OsInfo os = overview.os();
        assertNotNull(os.name());
        assertFalse(os.name().isEmpty());
        assertNotNull(os.arch());
        assertFalse(os.hostname().isEmpty());
        assertTrue(os.availableProcessors() > 0);

        // CPU
        ServerMonitorService.CpuInfo cpu = overview.cpu();
        assertTrue(cpu.availableProcessors() > 0);
        assertTrue(cpu.physicalCores() > 0);
        assertTrue(cpu.logicalCores() >= cpu.physicalCores(),
                "logical cores >= physical cores");

        // Memory
        ServerMonitorService.MemoryInfo mem = overview.memory();
        if (mem.totalPhysical() > 0) {
            assertTrue(mem.usedPhysical() >= 0);
            assertTrue(mem.usedPercent() >= 0 && mem.usedPercent() <= 100,
                    "memory usage 0-100%, got " + mem.usedPercent());
        }

        // Disk
        assertNotNull(overview.disks());
        assertFalse(overview.disks().isEmpty(), "at least one disk partition");

        // Network
        assertNotNull(overview.networks());
        assertFalse(overview.networks().isEmpty(), "at least one network interface");

        // Process
        ServerMonitorService.ProcessInfo proc = overview.process();
        assertTrue(proc.pid() > 0);
        assertTrue(proc.threadCount() > 0);
    }

    @Test
    @DisplayName("OS 信息字段完整")
    void testOsInfo() {
        ServerMonitorService.OsInfo os = overview.os();
        assertNotNull(os.jvmVendor());
        assertNotNull(os.jvmVersion());
        assertTrue(os.uptimeMs() >= 0);
        assertNotNull(os.uptimeFormatted());
        assertFalse(os.uptimeFormatted().isEmpty());
    }

    // ==================== CPU ====================

    @Test
    @DisplayName("CPU 详情包含摘要和每核信息")
    void testCpuDetail() {
        assertNotNull(cpuDetail);
        assertNotNull(cpuDetail.summary());
        assertNotNull(cpuDetail.summary().cpuModel());

        // 每核负载：Linux 可获取，macOS 可能为空
        assertNotNull(cpuDetail.perCoreLoad());
    }

    // ==================== Memory ====================

    @Test
    @DisplayName("内存详情包含 SWAP 和 JVM 堆")
    void testMemoryDetail() {
        assertNotNull(memoryDetail);
        assertNotNull(memoryDetail.summary());
        assertTrue(memoryDetail.jvmHeapUsed() > 0);
        assertTrue(memoryDetail.jvmHeapMax() > 0);
        assertTrue(memoryDetail.jvmHeapUsagePercent() >= 0);
    }

    @Test
    @DisplayName("物理内存使用量不超过总量")
    void testMemoryBounds() {
        ServerMonitorService.MemoryInfo mem = memoryDetail.summary();
        if (mem.totalPhysical() > 0) {
            assertTrue(mem.usedPhysical() <= mem.totalPhysical(),
                    "used <= total physical memory");
        }
        if (mem.totalSwap() > 0) {
            assertTrue(mem.usedSwap() <= mem.totalSwap(),
                    "used <= total swap");
        }
    }

    // ==================== Disk ====================

    @Test
    @DisplayName("磁盘分区统计包含必要字段")
    void testDiskDetail() {
        assertFalse(disks.isEmpty());
        for (ServerMonitorService.DiskInfo disk : disks) {
            assertNotNull(disk.mountPoint());
            assertTrue(disk.totalBytes() > 0, "total > 0 for " + disk.mountPoint());
            assertTrue(disk.usedBytes() >= 0);
            assertTrue(disk.usedPercent() >= 0 && disk.usedPercent() <= 100);
            assertNotNull(disk.totalDisplay());
            assertNotNull(disk.freeDisplay());
        }
    }

    @Test
    @DisplayName("磁盘分区按使用率降序")
    void testDiskSortedByUsage() {
        if (disks.size() < 2) return;
        assertTrue(disks.get(0).usedPercent() >= disks.get(1).usedPercent(),
                "disks sorted by usage DESC");
    }

    // ==================== Network ====================

    @Test
    @DisplayName("网络接口至少包含 loopback")
    void testNetworkHasLoopback() {
        boolean hasLoopback = networks.stream()
                .anyMatch(ServerMonitorService.NetworkIfInfo::loopback);
        assertTrue(hasLoopback, "should have loopback interface");
    }

    @Test
    @DisplayName("每个网络接口字段完整")
    void testNetworkFields() {
        assertFalse(networks.isEmpty());
        for (ServerMonitorService.NetworkIfInfo ni : networks) {
            assertNotNull(ni.name());
            assertFalse(ni.name().isEmpty());
            assertNotNull(ni.ipAddresses());
            // bytesReceived/Sent can be 0 on some interfaces
            assertTrue(ni.bytesReceived() >= 0);
            assertTrue(ni.bytesSent() >= 0);
        }
    }

    // ==================== Process ====================

    @Test
    @DisplayName("进程信息包含 PID 和线程数")
    void testProcessInfo() {
        ServerMonitorService.ProcessInfo proc = overview.process();
        assertEquals(ProcessHandle.current().pid(), proc.pid());
        assertTrue(proc.threadCount() > 0);
        assertNotNull(proc.command());
        assertFalse(proc.command().isEmpty());
    }

    // ==================== 一致性 ====================

    @Test
    @DisplayName("连续两次调用概览一致")
    void testOverviewConsistency() {
        ServerMonitorService.ServerOverview o2 = serverMonitorService.getOverview();
        assertEquals(overview.os().name(), o2.os().name());
        assertEquals(overview.os().arch(), o2.os().arch());
        assertEquals(overview.cpu().availableProcessors(), o2.cpu().availableProcessors());
        assertEquals(overview.process().pid(), o2.process().pid());
    }
}
