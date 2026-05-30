package org.example.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.ai.provider.AiModelProvider;
import org.example.ai.provider.AiModelRouter;
import org.example.service.DatabaseMonitorService;
import org.example.service.JvmMonitorService;
import org.example.service.ServerMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AiInspectService {

    private static final Logger log = LoggerFactory.getLogger(AiInspectService.class);

    private final AiModelRouter router;
    private final ObjectMapper objectMapper;
    private final JvmMonitorService jvmMonitor;
    private final DatabaseMonitorService dbMonitor;
    private final ServerMonitorService serverMonitor;

    public AiInspectService(
            AiModelRouter router,
            ObjectMapper objectMapper,
            JvmMonitorService jvmMonitor,
            DatabaseMonitorService dbMonitor,
            ServerMonitorService serverMonitor) {
        this.router = router;
        this.objectMapper = objectMapper;
        this.jvmMonitor = jvmMonitor;
        this.dbMonitor = dbMonitor;
        this.serverMonitor = serverMonitor;
    }

    public Map<String, Object> inspect(String target) {
        return inspect(target, null);
    }

    public Map<String, Object> inspect(String target, String providerName) {
        AiModelProvider provider = router.resolve(providerName);
        Map<String, Object> metrics = collectMetrics(target);
        String analysis = analyzeWithAI(metrics, target, provider);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("target", target);
        result.put("metrics", metrics);
        result.put("analysis", analysis);
        result.put("model", provider.getModel());
        return result;
    }

    private Map<String, Object> collectMetrics(String target) {
        Map<String, Object> data = new LinkedHashMap<>();
        try {
            if (target == null || "all".equals(target) || "jvm".equals(target)) {
                JvmMonitorService.JvmOverview overview = jvmMonitor.getOverview();
                Map<String, Object> jvm = new LinkedHashMap<>();
                jvm.put("jvmName", overview.jvmName());
                jvm.put("uptime", overview.uptimeFormatted());
                jvm.put("availableProcessors", overview.availableProcessors());
                jvm.put("systemLoadAverage", overview.systemLoadAverage());
                jvm.put("heapUsedMB", overview.memory().heapUsed() / 1024 / 1024);
                jvm.put("heapMaxMB", overview.memory().heapMax() / 1024 / 1024);
                jvm.put("heapUsagePercent", overview.memory().heapUsagePercent());
                jvm.put("threadCount", overview.threads().currentCount());
                jvm.put("virtualThreadCount", overview.threads().virtualCount());
                jvm.put("platformThreadCount", overview.threads().platformCount());
                jvm.put("peakThreadCount", overview.threads().peakCount());
                if (overview.gc() != null && overview.gc().warnings() != null) {
                    jvm.put("gcWarnings", overview.gc().warnings().warnings());
                }
                data.put("jvm", jvm);
            }
            if (target == null || "all".equals(target) || "db".equals(target)) {
                Map<String, Object> db = new LinkedHashMap<>();
                var poolDetail = dbMonitor.getConnectionPoolDetail();
                if (poolDetail != null && poolDetail.pool() != null) {
                    var pool = poolDetail.pool();
                    db.put("activeConnections", pool.activeConnections());
                    db.put("totalConnections", pool.totalConnections());
                    db.put("maxPoolSize", pool.maxPoolSize());
                    db.put("usagePercent", pool.usagePercent());
                    db.put("threadsAwaiting", pool.threadsAwaitingConnection());
                }
                List<DatabaseMonitorService.TableStat> tables = dbMonitor.getTableStats();
                long totalDeadTuples = 0;
                for (var t : tables) {
                    totalDeadTuples += t.nDeadTup();
                }
                db.put("tableCount", tables.size());
                db.put("totalDeadTuples", totalDeadTuples);
                data.put("db", db);
            }
            if (target == null || "all".equals(target) || "server".equals(target)) {
                ServerMonitorService.ServerOverview overview = serverMonitor.getOverview();
                Map<String, Object> server = new LinkedHashMap<>();
                if (overview.cpu() != null) {
                    server.put("cpuLoadPercent", overview.cpu().systemCpuLoadPercent());
                    server.put("processCpuLoadPercent", overview.cpu().processCpuLoadPercent());
                }
                if (overview.memory() != null) {
                    server.put("memoryUsedPercent", overview.memory().usedPercent());
                    server.put("swapUsedPercent", overview.memory().swapUsedPercent());
                }
                data.put("server", server);
            }
        } catch (Exception e) {
            log.warn("Failed to collect metrics for {}: {}", target, e.getMessage());
            data.put("error", e.getMessage());
        }
        return data;
    }

    private String analyzeWithAI(Map<String, Object> metrics, String target, AiModelProvider provider) {
        try {
            String metricsJson = objectMapper.writeValueAsString(metrics);
            String systemPrompt = """
                    你是一位资深的运维工程师和性能专家。请分析以下系统监控指标，给出：
                    1. 总体健康评估（正常/警告/严重）
                    2. 发现的问题（如有）
                    3. 改进建议
                    请用简洁专业的中文回答，控制在300字以内。""";

            Map<String, Object> requestBody = Map.of(
                    "model", provider.getModel(),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", "请分析以下监控指标：\n" + metricsJson)
                    ),
                    "max_tokens", 800,
                    "temperature", 0.3
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = provider.getRestClient().post()
                    .uri("/v1/chat/completions")
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("choices")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
            return "AI 分析暂时不可用";
        } catch (Exception e) {
            log.error("AI inspection analysis failed", e);
            return "AI 分析失败: " + e.getMessage();
        }
    }
}
