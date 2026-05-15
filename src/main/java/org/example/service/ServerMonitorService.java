package org.example.service;

import java.util.List;

/**
 * 服务器监控服务接口
 * 提供操作系统级指标：CPU、内存、磁盘、网络、进程
 */
public interface ServerMonitorService {

    /** 服务器综合概览 */
    ServerOverview getOverview();

    /** CPU 详情 */
    CpuDetail getCpuDetail();

    /** 内存详情 */
    MemoryDetail getMemoryDetail();

    /** 磁盘详情 */
    List<DiskInfo> getDiskDetail();

    /** 网络详情 */
    List<NetworkIfInfo> getNetworkDetail();

    // ==================== 数据模型 ====================

    record ServerOverview(
            OsInfo os,
            CpuInfo cpu,
            MemoryInfo memory,
            List<DiskInfo> disks,
            List<NetworkIfInfo> networks,
            ProcessInfo process,
            long timestamp
    ) {}

    record OsInfo(
            String name,
            String version,
            String arch,
            String hostname,
            long uptimeMs,
            String uptimeFormatted,
            int availableProcessors,
            double systemLoadAverage,
            String jvmVendor,
            String jvmVersion
    ) {}

    record CpuInfo(
            double systemCpuLoadPercent,
            double processCpuLoadPercent,
            double systemLoadAverage,
            int availableProcessors,
            int physicalCores,
            int logicalCores,
            String cpuModel
    ) {}

    record CpuDetail(
            CpuInfo summary,
            List<CpuCoreInfo> perCoreLoad
    ) {}

    record CpuCoreInfo(
            int coreIndex,
            double loadPercent
    ) {}

    record MemoryInfo(
            long totalPhysical,
            long freePhysical,
            long usedPhysical,
            double usedPercent,
            long totalSwap,
            long freeSwap,
            long usedSwap,
            double swapUsedPercent,
            long committedVirtualMemory,
            long processRss,
            long processVms
    ) {}

    record MemoryDetail(
            MemoryInfo summary,
            long jvmHeapUsed,
            long jvmHeapMax,
            double jvmHeapUsagePercent
    ) {}

    record DiskInfo(
            String mountPoint,
            String filesystem,
            long totalBytes,
            long freeBytes,
            long usableBytes,
            long usedBytes,
            double usedPercent,
            String totalDisplay,
            String usedDisplay,
            String freeDisplay
    ) {}

    record NetworkIfInfo(
            String name,
            String displayName,
            boolean up,
            boolean virtual,
            boolean loopback,
            String macAddress,
            List<String> ipAddresses,
            long bytesReceived,
            long bytesSent,
            long packetsReceived,
            long packetsSent,
            long errorsIn,
            long errorsOut,
            long dropIn,
            long dropOut
    ) {}

    record ProcessInfo(
            long pid,
            String processName,
            double cpuLoadPercent,
            long rssBytes,
            long vmsBytes,
            long openFileDescriptors,
            long maxFileDescriptors,
            long threadCount,
            String startTime,
            String user,
            String command
    ) {}
}
