package org.example.monitor.service;

import java.util.List;

public interface JvmProcessService {

    record HeapPoolInfo(String name, long capacity, long used, long max, double usagePercent) {}

    record GcStats(int youngGcCount, double youngGcTimeMs, int fullGcCount, double fullGcTimeMs) {}

    record JvmProcessSummary(
            String pid,
            String mainClass,
            String jvmArgs,
            String jvmVersion,
            String uptime,
            long uptimeMs,
            long heapUsedBytes,
            long heapMaxBytes
    ) {}

    record JvmProcessDetail(
            String pid,
            String mainClass,
            String jvmArgs,
            String jvmVersion,
            String javaHome,
            String uptime,
            long uptimeMs,
            String osInfo,
            List<HeapPoolInfo> heapPools,
            GcStats gcStats,
            int threadCount,
            List<String> vmFlags
    ) {}

    record JvmThreadDump(String pid, int threadCount, String rawDump) {}

    List<JvmProcessSummary> listProcesses();

    JvmProcessDetail getProcessDetail(String pid);

    JvmThreadDump getThreadDump(String pid);
}
