package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.ReconciliationDetail;
import org.example.entity.ReconciliationRecord;
import org.example.service.ReconciliationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/reconciliation")
@RequiredArgsConstructor
public class ReconciliationController {

    private final ReconciliationService reconciliationService;
    private static final java.util.Set<String> VALID_METHODS = java.util.Set.of("ALIPAY", "WECHAT");

    /**
     * 手动触发对帐
     */
    @PostMapping("/run")
    public Map<String, Object> runReconciliation(@RequestBody Map<String, Object> request) {
        LocalDate reconDate = request.containsKey("date")
                ? LocalDate.parse(request.get("date").toString())
                : LocalDate.now().minusDays(1);
        String paymentMethod = request.getOrDefault("paymentMethod", "ALIPAY").toString();

        if (!VALID_METHODS.contains(paymentMethod.toUpperCase())) {
            return Map.of("success", false, "message", "不支持的支付方式: " + paymentMethod + "，仅支持 ALIPAY / WECHAT",
                    "timestamp", System.currentTimeMillis());
        }

        log.info("手动触发对帐 - 日期: {}, 方式: {}", reconDate, paymentMethod);
        try {
            ReconciliationRecord record = reconciliationService.reconcile(reconDate, paymentMethod);
            return Map.of(
                    "success", true,
                    "data", record,
                    "message", "SUCCESS".equals(record.getStatus()) ? "对帐一致" : "存在差异，请查看详情",
                    "timestamp", System.currentTimeMillis()
            );
        } catch (RuntimeException e) {
            log.error("对帐失败 - {}", e.getMessage());
            return Map.of("success", false, "message", e.getMessage(), "timestamp", System.currentTimeMillis());
        }
    }

    /**
     * 异步对帐 - 虚拟线程
     */
    @PostMapping("/run/async")
    public CompletableFuture<Map<String, Object>> runReconciliationAsync(@RequestBody Map<String, Object> request) {
        LocalDate reconDate = request.containsKey("date")
                ? LocalDate.parse(request.get("date").toString())
                : LocalDate.now().minusDays(1);
        String paymentMethod = request.getOrDefault("paymentMethod", "ALIPAY").toString();

        if (!VALID_METHODS.contains(paymentMethod.toUpperCase())) {
            return CompletableFuture.completedFuture(
                    Map.of("success", false, "message", "不支持的支付方式: " + paymentMethod + "，仅支持 ALIPAY / WECHAT",
                            "timestamp", System.currentTimeMillis()));
        }

        log.info("异步触发对帐 - 日期: {}, 方式: {}", reconDate, paymentMethod);
        return reconciliationService.reconcileAsync(reconDate, paymentMethod)
                .thenApply(record -> Map.of(
                        "success", true,
                        "data", record,
                        "message", "SUCCESS".equals(record.getStatus()) ? "对帐一致" : "存在差异，请查看详情",
                        "timestamp", System.currentTimeMillis()
                ));
    }

    /**
     * 查询对帐记录
     */
    @GetMapping("/records")
    public Map<String, Object> getReconciliationRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<ReconciliationRecord> records = reconciliationService.getReconciliationRecordsByPage(page, size);

        return Map.of(
                "success", true,
                "data", records,
                "page", page,
                "size", size,
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 查询对帐记录详情
     */
    @GetMapping("/records/{id}")
    public Map<String, Object> getReconciliationRecord(@PathVariable Long id) {
        ReconciliationRecord record = reconciliationService.getReconciliationRecord(id);
        if (record == null) {
            return Map.of("success", false, "message", "对帐记录不存在", "timestamp", System.currentTimeMillis());
        }
        return Map.of("success", true, "data", record, "timestamp", System.currentTimeMillis());
    }

    /**
     * 查询对帐明细（逐笔对比）
     */
    @GetMapping("/details/{reconRecordId}")
    public Map<String, Object> getReconciliationDetails(@PathVariable Long reconRecordId) {
        List<ReconciliationDetail> details = reconciliationService.getReconciliationDetails(reconRecordId);

        long matchCount = details.stream().filter(d -> "MATCH".equals(d.getDiffType())).count();
        long mismatchCount = details.stream().filter(d -> "MISMATCH".equals(d.getDiffType())).count();
        long localOnlyCount = details.stream().filter(d -> "LOCAL_ONLY".equals(d.getDiffType())).count();
        long remoteOnlyCount = details.stream().filter(d -> "REMOTE_ONLY".equals(d.getDiffType())).count();

        return Map.of(
                "success", true,
                "data", details,
                "summary", Map.of(
                        "total", details.size(),
                        "match", matchCount,
                        "mismatch", mismatchCount,
                        "localOnly", localOnlyCount,
                        "remoteOnly", remoteOnlyCount
                ),
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 对帐统计
     */
    @GetMapping("/stats")
    public Map<String, Object> getReconciliationStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("查询对帐统计 - 开始: {}, 结束: {}", startDate, endDate);
        Map<String, Object> stats = reconciliationService.getReconciliationStats(startDate, endDate);
        return Map.of("success", true, "data", stats, "timestamp", System.currentTimeMillis());
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        return Map.of(
                "status", "UP",
                "service", "对帐服务",
                "schedule", "每日凌晨2:00自动对帐",
                "supportedMethods", new String[]{"ALIPAY", "WECHAT"},
                "timestamp", System.currentTimeMillis()
        );
    }
}
