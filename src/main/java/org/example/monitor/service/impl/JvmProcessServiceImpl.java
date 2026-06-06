package org.example.monitor.service.impl;

import org.example.monitor.service.JvmProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JvmProcessServiceImpl implements JvmProcessService {

    private static final Logger log = LoggerFactory.getLogger(JvmProcessServiceImpl.class);
    private static final int CMD_TIMEOUT_SEC = 5;

    @Override
    public List<JvmProcessSummary> listProcesses() {
        List<JvmProcessSummary> processes = new ArrayList<>();
        List<String[]> raw = getJavaProcesses();
        for (String[] proc : raw) {
            String pid = proc[0];
            String mainClass = proc[1];
            String jvmArgs = proc[2];

            if (isToolProcess(mainClass)) continue;

            JvmProcessSummary summary = buildSummary(pid, mainClass, jvmArgs);
            if (summary != null) {
                processes.add(summary);
            }
        }
        processes.sort(Comparator.comparingLong(JvmProcessSummary::uptimeMs).reversed());
        return processes;
    }

    @Override
    public JvmProcessDetail getProcessDetail(String pid) {
        String[] procInfo = getProcessInfo(pid);
        String mainClass = procInfo[0];
        String jvmArgs = procInfo[1];

        String vmInfoOut = execWithTimeout(new String[]{"jcmd", pid, "VM.info"}, CMD_TIMEOUT_SEC);
        String vmUptimeOut = execWithTimeout(new String[]{"jcmd", pid, "VM.uptime"}, CMD_TIMEOUT_SEC);
        String vmFlagsOut = execWithTimeout(new String[]{"jcmd", pid, "VM.flags"}, CMD_TIMEOUT_SEC);

        String jvmVersion = parseJvmVersion(vmInfoOut);
        String javaHome = parseJavaHome(execWithTimeout(new String[]{"jcmd", pid, "VM.system_properties"}, CMD_TIMEOUT_SEC));
        String osInfo = parseOsInfo(vmInfoOut);
        long uptimeMs = parseUptime(vmUptimeOut);
        String uptime = formatUptime(uptimeMs);
        int threadCount = parseThreadCount(pid);
        List<String> vmFlags = parseVmFlags(vmFlagsOut);
        String jstatOut = execWithTimeout(new String[]{"jstat", "-gc", pid}, CMD_TIMEOUT_SEC);
        List<HeapPoolInfo> heapPools = parseHeapPools(jstatOut);
        GcStats gcStats = parseGcStats(jstatOut);

        return new JvmProcessDetail(pid, mainClass, jvmArgs, jvmVersion, javaHome,
                uptime, uptimeMs, osInfo, heapPools, gcStats, threadCount, vmFlags);
    }

    @Override
    public JvmThreadDump getThreadDump(String pid) {
        String out = execWithTimeout(new String[]{"jcmd", pid, "Thread.print"}, CMD_TIMEOUT_SEC);
        if (out == null) {
            return new JvmThreadDump(pid, 0, "无法获取线程转储");
        }
        int count = 0;
        for (String line : out.split("\n")) {
            if (line.trim().startsWith("\"")) count++;
        }
        return new JvmThreadDump(pid, count, out);
    }

    // ==================== Private helpers ====================

    private JvmProcessSummary buildSummary(String pid, String mainClass, String jvmArgs) {
        String vmInfoOut = execWithTimeout(new String[]{"jcmd", pid, "VM.info"}, CMD_TIMEOUT_SEC);
        String vmUptimeOut = execWithTimeout(new String[]{"jcmd", pid, "VM.uptime"}, CMD_TIMEOUT_SEC);
        String jstatOut = execWithTimeout(new String[]{"jstat", "-gc", pid}, CMD_TIMEOUT_SEC);

        String jvmVersion = parseJvmVersion(vmInfoOut);
        long uptimeMs = parseUptime(vmUptimeOut);
        String uptime = formatUptime(uptimeMs);
        long heapUsed = 0;
        long heapMax = 0;

        if (jstatOut != null) {
            String[] lines = jstatOut.split("\n");
            if (lines.length >= 2) {
                String[] cols = lines[1].trim().split("\\s+");
                if (cols.length >= 18) {
                    try {
                        double s0u = Double.parseDouble(cols[2]); // S0U
                        double s1u = Double.parseDouble(cols[3]); // S1U
                        double eu = Double.parseDouble(cols[5]);  // EU
                        double ou = Double.parseDouble(cols[7]);  // OU
                        double s0c = Double.parseDouble(cols[0]); // S0C
                        double s1c = Double.parseDouble(cols[1]); // S1C
                        double ec = Double.parseDouble(cols[4]);  // EC
                        double oc = Double.parseDouble(cols[6]);  // OC
                        heapUsed = (long) ((ou + eu + s0u + s1u) * 1024);
                        heapMax = (long) ((s0c + s1c + ec + oc) * 1024);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        return new JvmProcessSummary(pid, mainClass, jvmArgs, jvmVersion, uptime,
                uptimeMs, heapUsed, heapMax);
    }

    private GcStats parseGcStats(String jstatOut) {
        if (jstatOut == null) return new GcStats(0, 0, 0, 0);

        String[] lines = jstatOut.split("\n");
        if (lines.length < 2) return new GcStats(0, 0, 0, 0);

        String[] cols = lines[1].trim().split("\\s+");
        if (cols.length < 18) return new GcStats(0, 0, 0, 0);

        try {
            int ygc = Integer.parseInt(cols[12]);  // YGC
            double ygct = Double.parseDouble(cols[13]); // YGCT (seconds)
            int fgc = Integer.parseInt(cols[14]);  // FGC
            double fgct = Double.parseDouble(cols[15]); // FGCT (seconds)
            return new GcStats(ygc, ygct * 1000, fgc, fgct * 1000);
        } catch (NumberFormatException e) {
            return new GcStats(0, 0, 0, 0);
        }
    }

    private List<HeapPoolInfo> parseHeapPools(String jstatOut) {
        List<HeapPoolInfo> pools = new ArrayList<>();
        if (jstatOut == null) return pools;

        String[] lines = jstatOut.split("\n");
        if (lines.length < 2) return pools;

        String[] cols = lines[1].trim().split("\\s+");
        if (cols.length < 18) return pools;

        try {
            double s0c = Double.parseDouble(cols[0]), s0u = Double.parseDouble(cols[2]);
            double s1c = Double.parseDouble(cols[1]), s1u = Double.parseDouble(cols[3]);
            double ec = Double.parseDouble(cols[4]), eu = Double.parseDouble(cols[5]);
            double oc = Double.parseDouble(cols[6]), ou = Double.parseDouble(cols[7]);
            double mc = Double.parseDouble(cols[8]), mu = Double.parseDouble(cols[9]);

            long s0cB = (long) (s0c * 1024), s0uB = (long) (s0u * 1024);
            long s1cB = (long) (s1c * 1024), s1uB = (long) (s1u * 1024);
            long ecB = (long) (ec * 1024), euB = (long) (eu * 1024);
            long ocB = (long) (oc * 1024), ouB = (long) (ou * 1024);
            long mcB = (long) (mc * 1024), muB = (long) (mu * 1024);

            // Survivor (combined S0+S1), Eden, Old Gen, Metaspace
            pools.add(new HeapPoolInfo("Survivor", s0cB + s1cB, s0uB + s1uB, -1,
                    (s0cB + s1cB) > 0 ? ((s0uB + s1uB) * 100.0 / (s0cB + s1cB)) : 0));
            pools.add(new HeapPoolInfo("Eden", ecB, euB, -1,
                    ecB > 0 ? (euB * 100.0 / ecB) : 0));
            pools.add(new HeapPoolInfo("Old Gen", ocB, ouB, -1,
                    ocB > 0 ? (ouB * 100.0 / ocB) : 0));
            pools.add(new HeapPoolInfo("Metaspace", mcB, muB, -1,
                    mcB > 0 ? (muB * 100.0 / mcB) : 0));
        } catch (NumberFormatException ignored) {}
        return pools;
    }

    private int parseThreadCount(String pid) {
        String out = execWithTimeout(new String[]{"jcmd", pid, "Thread.print", "-e"}, CMD_TIMEOUT_SEC);
        if (out == null) return -1;
        int count = 0;
        for (String line : out.split("\n")) {
            if (line.trim().startsWith("\"")) count++;
        }
        return count;
    }

    private List<String> parseVmFlags(String flagsOut) {
        List<String> flags = new ArrayList<>();
        if (flagsOut == null) return flags;

        boolean inFlags = false;
        for (String line : flagsOut.split("\n")) {
            line = line.trim();
            if (line.startsWith("-XX:") || line.startsWith("-Xm") || line.startsWith("-D")) {
                if (line.endsWith(",")) line = line.substring(0, line.length() - 1);
                flags.add(line);
            }
        }
        return flags;
    }

    private List<String[]> getJavaProcesses() {
        List<String[]> processes = new ArrayList<>();
        String out = execWithTimeout(new String[]{"jps", "-lv"}, CMD_TIMEOUT_SEC);
        if (out == null) return processes;

        for (String line : out.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // Parse: PID MainClass JVMArgs...
            String[] parts = line.split("\\s+", 3);
            if (parts.length < 2) continue;

            String pid = parts[0];
            String mainClass = parts[1];
            String jvmArgs = parts.length >= 3 ? parts[2] : "";

            // Filter out jps itself
            if (mainClass.equals("jdk.jcmd/sun.tools.jps.Jps") ||
                mainClass.equals("jdk.jcmd/sun.tools.jstat.Jstat") ||
                mainClass.equals("jdk.jcmd/sun.tools.jcmd.JCmd") ||
                mainClass.contains("sun.tools.jps")) {
                continue;
            }

            processes.add(new String[]{pid, mainClass, jvmArgs});
        }
        return processes;
    }

    private String[] getProcessInfo(String pid) {
        String out = execWithTimeout(new String[]{"jps", "-lv"}, CMD_TIMEOUT_SEC);
        if (out != null) {
            for (String line : out.split("\n")) {
                line = line.trim();
                if (line.startsWith(pid + " ")) {
                    String[] parts = line.split("\\s+", 3);
                    return new String[]{
                            parts.length >= 2 ? parts[1] : "unknown",
                            parts.length >= 3 ? parts[2] : ""
                    };
                }
            }
        }
        return new String[]{"unknown", ""};
    }

    private boolean isToolProcess(String mainClass) {
        return mainClass.contains("jdk.jcmd") || mainClass.contains("sun.tools");
    }

    private String parseJvmVersion(String vmInfoOut) {
        if (vmInfoOut == null) return "N/A";
        for (String line : vmInfoOut.split("\n")) {
            if (line.contains("JRE version:")) {
                // "# JRE version: Java(TM) SE Runtime Environment (25.0.3+9) (build 25.0.3+9-LTS-195)"
                // Use non-greedy .*? after "JRE version:" to capture the first version parens
                Matcher m = Pattern.compile("JRE version:.*?\\(([0-9]+\\.[0-9]+\\.[0-9]+[^)]*)\\)").matcher(line);
                if (m.find()) return m.group(1);
            }
            if (line.contains("Java HotSpot")) {
                return line.replaceAll(".*\\(([^)]+)\\).*$", "$1");
            }
        }
        return "N/A";
    }

    private String parseJavaHome(String sysPropsOut) {
        if (sysPropsOut == null) return "N/A";
        for (String line : sysPropsOut.split("\n")) {
            if (line.startsWith("java.home=")) {
                return line.substring("java.home=".length()).trim();
            }
        }
        return "N/A";
    }

    private String parseOsInfo(String vmInfoOut) {
        if (vmInfoOut == null) return "N/A";
        for (String line : vmInfoOut.split("\n")) {
            if (line.startsWith("Host:")) {
                return line.substring(5).trim();
            }
        }
        return "N/A";
    }

    private long parseUptime(String vmUptimeOut) {
        if (vmUptimeOut == null) return -1;
        try {
            // Output format: "<pid>:\n<seconds> s"
            for (String line : vmUptimeOut.split("\n")) {
                line = line.trim();
                if (line.isEmpty() || line.endsWith(":")) continue;
                String[] parts = line.split("\\s+");
                if (parts.length >= 1) {
                    double seconds = Double.parseDouble(parts[0]);
                    return (long) (seconds * 1000);
                }
            }
        } catch (NumberFormatException ignored) {}
        return -1;
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

    private String execWithTimeout(String[] cmd, int timeoutSec) {
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            // Read output concurrently to avoid pipe buffer deadlock for large outputs
            CompletableFuture<String> outputFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return new String(p.getInputStream().readAllBytes());
                } catch (IOException e) {
                    return null;
                }
            });
            boolean done = p.waitFor(timeoutSec, TimeUnit.SECONDS);
            if (!done) {
                p.destroyForcibly();
                log.debug("Command timed out: {}", String.join(" ", cmd));
                return null;
            }
            String output = outputFuture.get(1, TimeUnit.SECONDS);
            if (p.exitValue() != 0) {
                log.debug("Command failed (exit {}): {} — {}", p.exitValue(), String.join(" ", cmd),
                        output != null ? output.trim() : "");
                return output;
            }
            return output;
        } catch (Exception e) {
            log.debug("Command failed: {} — {}", String.join(" ", cmd), e.getMessage());
            return null;
        }
    }
}
