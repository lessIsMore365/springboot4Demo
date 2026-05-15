package org.example.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.service.ServerMonitorService;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * 服务器监控服务实现
 * 跨平台（Linux/macOS）采集系统级指标
 */
@Slf4j
@Service
public class ServerMonitorServiceImpl implements ServerMonitorService {

    private static final boolean IS_LINUX = System.getProperty("os.name", "").toLowerCase().contains("linux");
    private static final boolean IS_MAC = System.getProperty("os.name", "").toLowerCase().contains("mac");

    @Override
    public ServerOverview getOverview() {
        OsInfo os = buildOsInfo();
        CpuInfo cpu = buildCpuInfo();
        MemoryInfo mem = buildMemoryInfo();
        List<DiskInfo> disks = buildDiskInfo();
        List<NetworkIfInfo> networks = buildNetworkInfo();
        ProcessInfo proc = buildProcessInfo();

        return new ServerOverview(os, cpu, mem, disks, networks, proc, System.currentTimeMillis());
    }

    @Override
    public CpuDetail getCpuDetail() {
        CpuInfo summary = buildCpuInfo();
        List<CpuCoreInfo> perCore = buildPerCoreLoad();
        return new CpuDetail(summary, perCore);
    }

    @Override
    public MemoryDetail getMemoryDetail() {
        MemoryInfo summary = buildMemoryInfo();
        var memoryBean = ManagementFactory.getMemoryMXBean();
        var heap = memoryBean.getHeapMemoryUsage();
        return new MemoryDetail(
                summary,
                heap.getUsed(),
                heap.getMax() > 0 ? heap.getMax() : heap.getCommitted(),
                heap.getMax() > 0 ? (double) heap.getUsed() / heap.getMax() * 100 : 0
        );
    }

    @Override
    public List<DiskInfo> getDiskDetail() {
        return buildDiskInfo();
    }

    @Override
    public List<NetworkIfInfo> getNetworkDetail() {
        return buildNetworkInfo();
    }

    // ==================== OS ====================

    private OsInfo buildOsInfo() {
        String name = System.getProperty("os.name");
        String version = System.getProperty("os.version");
        String arch = System.getProperty("os.arch");
        String hostname = getHostname();

        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        long uptimeMs = 0;
        try {
            // JVM uptime as approximation; real OS uptime from /proc/uptime or sysctl
            if (IS_LINUX) {
                String s = Files.readString(Path.of("/proc/uptime"));
                uptimeMs = (long) (Double.parseDouble(s.split("\\s+")[0]) * 1000);
            } else if (IS_MAC) {
                var pb = new ProcessBuilder("sysctl", "-n", "kern.boottime").start();
                String out = new String(pb.getInputStream().readAllBytes()).trim();
                // { sec = 12345, usec = 0 } -> extract sec
                long bootSec = Long.parseLong(out.replaceAll("[^0-9]", " ").trim().split("\\s+")[0]);
                uptimeMs = System.currentTimeMillis() - bootSec * 1000;
            }
        } catch (Exception e) {
            // fallback to JVM uptime
            uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        }

        String jvmVendor = System.getProperty("java.vendor");
        String jvmVersion = System.getProperty("java.version");

        return new OsInfo(
                name, version, arch, hostname, uptimeMs, formatUptime(uptimeMs),
                Runtime.getRuntime().availableProcessors(),
                osBean.getSystemLoadAverage(),
                jvmVendor, jvmVersion
        );
    }

    // ==================== CPU ====================

    private CpuInfo buildCpuInfo() {
        double systemLoad = -1, processLoad = -1, loadAvg = -1;
        int cores = Runtime.getRuntime().availableProcessors();

        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        loadAvg = osBean.getSystemLoadAverage();

        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOs) {
            systemLoad = sunOs.getCpuLoad() * 100;
            processLoad = sunOs.getProcessCpuLoad() * 100;
        }

        String cpuModel = getCpuModel();
        int physicalCores = cores; // 保守估计
        int logicalCores = cores;
        if (IS_LINUX) {
            try {
                String cpuinfo = Files.readString(Path.of("/proc/cpuinfo"));
                physicalCores = (int) cpuinfo.lines().filter(l -> l.startsWith("cpu cores")).count();
                logicalCores = (int) cpuinfo.lines().filter(l -> l.startsWith("processor")).count();
                if (physicalCores == 0) physicalCores = cores;
            } catch (Exception ignored) {}
        } else if (IS_MAC) {
            try {
                var pb = new ProcessBuilder("sysctl", "-n", "hw.physicalcpu").start();
                physicalCores = Integer.parseInt(new String(pb.getInputStream().readAllBytes()).trim());
                pb = new ProcessBuilder("sysctl", "-n", "hw.logicalcpu").start();
                logicalCores = Integer.parseInt(new String(pb.getInputStream().readAllBytes()).trim());
            } catch (Exception ignored) {}
        }

        return new CpuInfo(systemLoad, processLoad, loadAvg, cores, physicalCores, logicalCores, cpuModel);
    }

    private List<CpuCoreInfo> buildPerCoreLoad() {
        List<CpuCoreInfo> list = new ArrayList<>();
        try {
            if (IS_LINUX) {
                String stat = Files.readString(Path.of("/proc/stat"));
                int idx = 0;
                for (String line : stat.lines().toList()) {
                    if (line.startsWith("cpu") && !line.equals("cpu") && !line.startsWith("cpu ")) {
                        // cpu0 231 0 5123 ... -> user nice system idle iowait irq softirq steal
                        String[] parts = line.trim().split("\\s+");
                        long total = 0, idle = 0;
                        for (int i = 1; i < parts.length; i++) {
                            long v = Long.parseLong(parts[i]);
                            total += v;
                            if (i == 4 || i == 5) idle += v; // idle + iowait
                        }
                        double load = total > 0 ? (1.0 - (double) idle / total) * 100 : 0;
                        list.add(new CpuCoreInfo(idx++, Math.round(load * 10.0) / 10.0));
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Per-core CPU load not available: {}", e.getMessage());
        }
        return list;
    }

    // ==================== Memory ====================

    private MemoryInfo buildMemoryInfo() {
        long totalPhysical = 0, freePhysical = 0, totalSwap = 0, freeSwap = 0;
        long committedVirtual = 0, processRss = 0, processVms = 0;

        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOs) {
            totalPhysical = sunOs.getTotalMemorySize();
            freePhysical = sunOs.getFreeMemorySize();
            totalSwap = sunOs.getTotalSwapSpaceSize();
            freeSwap = sunOs.getFreeSwapSpaceSize();
            committedVirtual = sunOs.getCommittedVirtualMemorySize();
        }

        // 进程内存
        try {
            if (IS_LINUX) {
                String status = Files.readString(Path.of("/proc/self/status"));
                for (String line : status.split("\n")) {
                    if (line.startsWith("VmRSS:")) {
                        processRss = Long.parseLong(line.replaceAll("[^0-9]", "")) * 1024;
                    } else if (line.startsWith("VmSize:")) {
                        processVms = Long.parseLong(line.replaceAll("[^0-9]", "")) * 1024;
                    }
                }
            } else if (IS_MAC) {
                long pid = ProcessHandle.current().pid();
                String out = execWithTimeout(new String[]{"ps", "-o", "rss,vsize", "-p", String.valueOf(pid)}, 5);
                if (out != null) {
                    String[] lines = out.split("\n");
                    if (lines.length > 1) {
                        String[] vals = lines[1].trim().split("\\s+");
                        if (vals.length >= 2) {
                            processRss = Long.parseLong(vals[0]) * 1024;
                            processVms = Long.parseLong(vals[1]) * 1024;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Process memory info not available: {}", e.getMessage());
        }

        long usedPhysical = totalPhysical - freePhysical;
        long usedSwap = totalSwap - freeSwap;

        return new MemoryInfo(
                totalPhysical, freePhysical, usedPhysical,
                totalPhysical > 0 ? (double) usedPhysical / totalPhysical * 100 : 0,
                totalSwap, freeSwap, usedSwap,
                totalSwap > 0 ? (double) usedSwap / totalSwap * 100 : 0,
                committedVirtual, processRss, processVms
        );
    }

    // ==================== Disk ====================

    private List<DiskInfo> buildDiskInfo() {
        List<DiskInfo> list = new ArrayList<>();
        for (File root : File.listRoots()) {
            try {
                long total = root.getTotalSpace();
                long free = root.getFreeSpace();
                long usable = root.getUsableSpace();
                long used = total - free;
                double pct = total > 0 ? (double) used / total * 100 : 0;

                String mount = root.getAbsolutePath();
                String fs = getFilesystemType(mount);

                list.add(new DiskInfo(
                        mount, fs, total, free, usable, used, pct,
                        formatBytes(total), formatBytes(used), formatBytes(free)
                ));
            } catch (Exception ignored) {}
        }
        // 按使用率降序
        list.sort(Comparator.comparingDouble(DiskInfo::usedPercent).reversed());
        return list;
    }

    // ==================== Network ====================

    private List<NetworkIfInfo> buildNetworkInfo() {
        List<NetworkIfInfo> list = new ArrayList<>();
        // 从 /proc/net/dev (Linux) 或 NetworkInterface 获取流量统计
        Map<String, long[]> trafficStats = readNetworkTraffic();

        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces.hasMoreElements()) {
                NetworkInterface ni = ifaces.nextElement();
                String name = ni.getName();
                long[] stats = trafficStats.getOrDefault(name, new long[]{0, 0, 0, 0, 0, 0, 0, 0});

                List<String> ips = new ArrayList<>();
                ni.getInetAddresses().asIterator().forEachRemaining(addr -> ips.add(addr.getHostAddress()));

                String mac = "";
                try {
                    byte[] macBytes = ni.getHardwareAddress();
                    if (macBytes != null) {
                        StringBuilder sb = new StringBuilder();
                        for (byte b : macBytes) sb.append(String.format("%02x:", b));
                        if (!sb.isEmpty()) sb.setLength(sb.length() - 1);
                        mac = sb.toString();
                    }
                } catch (SocketException ignored) {}

                boolean up = false, virtual = false;
                try { up = ni.isUp(); } catch (Exception ignored) {}

                list.add(new NetworkIfInfo(
                        name, ni.getDisplayName(),
                        up, virtual, ni.isLoopback(),
                        mac, ips,
                        stats[0], stats[1], stats[2], stats[3],
                        stats[4], stats[5], stats[6], stats[7]
                ));
            }
        } catch (SocketException e) {
            log.warn("Failed to enumerate network interfaces: {}", e.getMessage());
        }

        // 按总流量降序
        list.sort(Comparator.comparingLong(n -> -(n.bytesReceived() + n.bytesSent())));
        return list;
    }

    private Map<String, long[]> readNetworkTraffic() {
        Map<String, long[]> map = new LinkedHashMap<>();
        try {
            if (IS_LINUX) {
                String content = Files.readString(Path.of("/proc/net/dev"));
                for (String line : content.split("\n")) {
                    if (!line.contains(":")) continue;
                    String[] parts = line.trim().split("[\\s:]+");
                    if (parts.length < 17) continue;
                    String name = parts[0];
                    // bytes rx, pkts rx, errs rx, drop rx, ... bytes tx, pkts tx, errs tx, drop tx
                    long[] stats = {
                        Long.parseLong(parts[1]),  // bytesReceived
                        Long.parseLong(parts[9]),  // bytesSent
                        Long.parseLong(parts[2]),  // packetsReceived
                        Long.parseLong(parts[10]), // packetsSent
                        Long.parseLong(parts[3]),  // errorsIn
                        Long.parseLong(parts[11]), // errorsOut
                        Long.parseLong(parts[4]),  // dropIn
                        Long.parseLong(parts[12])  // dropOut
                    };
                    map.put(name, stats);
                }
            } else if (IS_MAC) {
                String content = execWithTimeout(new String[]{"netstat", "-ibn"}, 5);
                if (content != null) {
                    for (String line : content.split("\n")) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("Name")) continue;
                        String[] parts = line.split("\\s+");
                        if (parts.length < 10) continue;
                        String name = parts[0];
                        try {
                            long ibytes = Long.parseLong(parts[6]);
                            long obytes = Long.parseLong(parts[9]);
                            long ipkts = Long.parseLong(parts[4]);
                            long opkts = Long.parseLong(parts[7]);
                            long ierrs = Long.parseLong(parts[5]);
                            long oerrs = Long.parseLong(parts[8]);
                            map.put(name, new long[]{ibytes, obytes, ipkts, opkts, ierrs, oerrs, 0, 0});
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {}
        return map;
    }

    // ==================== Process ====================

    private ProcessInfo buildProcessInfo() {
        long pid = ProcessHandle.current().pid();
        ProcessHandle.Info info = ProcessHandle.current().info();
        String processName = info.command().orElse("java");
        String user = info.user().orElse("-");

        double cpuLoad = -1;
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOs) {
            cpuLoad = sunOs.getProcessCpuLoad() * 100;
        }

        long rss = 0, vms = 0;
        try {
            if (IS_LINUX) {
                String status = Files.readString(Path.of("/proc/self/status"));
                for (String line : status.split("\n")) {
                    if (line.startsWith("VmRSS:")) rss = Long.parseLong(line.replaceAll("[^0-9]", "")) * 1024;
                    if (line.startsWith("VmSize:")) vms = Long.parseLong(line.replaceAll("[^0-9]", "")) * 1024;
                }
            } else if (IS_MAC) {
                String out = execWithTimeout(new String[]{"ps", "-o", "rss,vsize", "-p", String.valueOf(pid)}, 5);
                if (out != null) {
                    String[] lines = out.split("\n");
                    if (lines.length > 1) {
                        String[] vals = lines[1].trim().split("\\s+");
                        if (vals.length >= 2) {
                            rss = Long.parseLong(vals[0]) * 1024;
                            vms = Long.parseLong(vals[1]) * 1024;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Process memory from OS: {}", e.getMessage());
        }

        long openFds = countOpenFileDescriptors();
        long maxFds = getMaxFileDescriptors();

        long threadCount = Thread.activeCount();
        try {
            threadCount = ManagementFactory.getThreadMXBean().getThreadCount();
        } catch (Exception ignored) {}

        String startTime = info.startInstant()
                .map(i -> i.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .orElse("-");

        String command;
        try {
            if (IS_LINUX) {
                command = Files.readString(Path.of("/proc/self/cmdline")).replace('\0', ' ').trim();
            } else {
                command = ProcessHandle.current().info().commandLine().orElse("java");
            }
        } catch (Exception e) {
            command = info.commandLine().orElse("java");
        }

        return new ProcessInfo(pid, processName, cpuLoad, rss, vms, openFds, maxFds,
                threadCount, startTime, user, command);
    }

    // ==================== Helpers ====================

    private String getHostname() {
        try { return InetAddress.getLocalHost().getHostName(); }
        catch (Exception e) { return "unknown"; }
    }

    private String getCpuModel() {
        try {
            if (IS_LINUX) {
                String cpuinfo = Files.readString(Path.of("/proc/cpuinfo"));
                for (String line : cpuinfo.split("\n")) {
                    if (line.startsWith("model name")) {
                        return line.split(":", 2)[1].trim();
                    }
                }
            } else if (IS_MAC) {
                var pb = new ProcessBuilder("sysctl", "-n", "machdep.cpu.brand_string").start();
                return new String(pb.getInputStream().readAllBytes()).trim();
            }
        } catch (Exception ignored) {}
        return System.getProperty("os.arch", "unknown");
    }

    private String getFilesystemType(String mount) {
        if (IS_LINUX) {
            try {
                for (String line : Files.readAllLines(Path.of("/proc/mounts"))) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 3 && parts[1].equals(mount)) {
                        return parts[2];
                    }
                }
            } catch (Exception ignored) {}
        }
        return "";
    }

    private long countOpenFileDescriptors() {
        try {
            if (IS_LINUX) {
                try (Stream<Path> files = Files.list(Path.of("/proc/self/fd"))) {
                    return files.count();
                }
            } else if (IS_MAC) {
                long pid = ProcessHandle.current().pid();
                String out = execWithTimeout(new String[]{"lsof", "-p", String.valueOf(pid)}, 5);
                return out != null ? out.lines().count() - 1 : -1;
            }
        } catch (Exception e) {
            log.debug("Cannot count open FDs: {}", e.getMessage());
        }
        return -1;
    }

    private long getMaxFileDescriptors() {
        try {
            if (IS_LINUX) {
                String limits = Files.readString(Path.of("/proc/self/limits"));
                for (String line : limits.split("\n")) {
                    if (line.contains("open files")) {
                        String[] parts = line.trim().split("\\s+");
                        return parts.length >= 4 ? Long.parseLong(parts[3]) : -1;
                    }
                }
            } else if (IS_MAC) {
                String out = execWithTimeout(new String[]{"launchctl", "limit", "maxfiles"}, 3);
                if (out != null) {
                    out = out.trim();
                    String[] parts = out.split("\\s+");
                    if (parts.length >= 3 && parts[parts.length - 1].equals("unlimited")) {
                        return Long.MAX_VALUE;
                    }
                    return parts.length >= 3 ? Long.parseLong(parts[parts.length - 1]) : -1;
                }
            }
        } catch (Exception e) {
            log.debug("Cannot get max FDs: {}", e.getMessage());
        }
        return -1;
    }

    private String formatBytes(long bytes) {
        if (bytes < 0) return "N/A";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1_048_576) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1_073_741_824) return String.format("%.1f MB", bytes / 1_048_576.0);
        if (bytes < 1_099_511_627_776L) return String.format("%.1f GB", bytes / 1_073_741_824.0);
        return String.format("%.2f TB", bytes / 1_099_511_627_776.0);
    }

    private String execWithTimeout(String[] cmd, int timeoutSec) {
        try {
            Process p = new ProcessBuilder(cmd).start();
            boolean done = p.waitFor(timeoutSec, TimeUnit.SECONDS);
            if (!done) {
                p.destroyForcibly();
                return null;
            }
            if (p.exitValue() != 0) return null;
            return new String(p.getInputStream().readAllBytes());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String formatUptime(long ms) {
        if (ms <= 0) return "N/A";
        long dayMs = 86_400_000;
        long days = ms / dayMs;
        long hours = (ms % dayMs) / 3_600_000;
        long minutes = (ms % 3_600_000) / 60_000;
        if (days > 0) return String.format("%dd %dh %dm", days, hours, minutes);
        if (hours > 0) return String.format("%dh %dm", hours, minutes);
        return String.format("%dm %ds", minutes, (ms % 60_000) / 1_000);
    }
}
