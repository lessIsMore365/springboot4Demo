package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.ApiResponse;
import org.example.entity.ReconciliationDetail;
import org.example.entity.ReconciliationRecord;
import org.example.service.ReconciliationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/reconciliation")
@RequiredArgsConstructor
@Tag(name = "对帐管理", description = "支付宝/微信支付自动对帐、差异明细查询、对帐统计")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;
    private static final Set<String> VALID_METHODS = Set.of("ALIPAY", "WECHAT");

    @PostMapping("/run")
    @Operation(summary = "手动触发对帐", description = "对指定日期的订单进行逐笔比对，生成对帐记录和差异明细")
    public ApiResponse<?> runReconciliation(@RequestBody Map<String, Object> request) {
        LocalDate reconDate = request.containsKey("date")
                ? LocalDate.parse(request.get("date").toString())
                : LocalDate.now().minusDays(1);
        String paymentMethod = request.getOrDefault("paymentMethod", "ALIPAY").toString();

        if (!VALID_METHODS.contains(paymentMethod.toUpperCase())) {
            return ApiResponse.fail("不支持的支付方式: " + paymentMethod + "，仅支持 ALIPAY / WECHAT");
        }

        log.info("手动触发对帐 - 日期: {}, 方式: {}", reconDate, paymentMethod);
        try {
            ReconciliationRecord record = reconciliationService.reconcile(reconDate, paymentMethod);
            return ApiResponse.ok(record, "SUCCESS".equals(record.getStatus()) ? "对帐一致" : "存在差异，请查看详情");
        } catch (RuntimeException e) {
            log.error("对帐失败 - {}", e.getMessage());
            return ApiResponse.fail(e.getMessage());
        }
    }

    @PostMapping("/run/async")
    @Operation(summary = "异步触发对帐", description = "通过虚拟线程异步执行对帐")
    public CompletableFuture<ApiResponse<?>> runReconciliationAsync(@RequestBody Map<String, Object> request) {
        LocalDate reconDate = request.containsKey("date")
                ? LocalDate.parse(request.get("date").toString())
                : LocalDate.now().minusDays(1);
        String paymentMethod = request.getOrDefault("paymentMethod", "ALIPAY").toString();

        if (!VALID_METHODS.contains(paymentMethod.toUpperCase())) {
            return CompletableFuture.completedFuture(
                    ApiResponse.fail("不支持的支付方式: " + paymentMethod + "，仅支持 ALIPAY / WECHAT"));
        }

        log.info("异步触发对帐 - 日期: {}, 方式: {}", reconDate, paymentMethod);
        return reconciliationService.reconcileAsync(reconDate, paymentMethod)
                .thenApply(record -> ApiResponse.ok(record,
                        "SUCCESS".equals(record.getStatus()) ? "对帐一致" : "存在差异，请查看详情"));
    }

    @GetMapping("/records")
    @Operation(summary = "分页查询对帐记录")
    public ApiResponse<List<ReconciliationRecord>> getReconciliationRecords(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        List<ReconciliationRecord> records = reconciliationService.getReconciliationRecordsByPage(page, size);
        return ApiResponse.ok(records);
    }

    @GetMapping("/records/{id}")
    @Operation(summary = "查询对帐记录详情")
    public ApiResponse<?> getReconciliationRecord(
            @Parameter(description = "对帐记录ID") @PathVariable Long id) {
        ReconciliationRecord record = reconciliationService.getReconciliationRecord(id);
        if (record == null) {
            return ApiResponse.fail("对帐记录不存在");
        }
        return ApiResponse.ok(record);
    }

    @GetMapping("/details/{reconRecordId}")
    @Operation(summary = "查询对帐明细", description = "逐笔比对结果，含 MATCH/MISMATCH/LOCAL_ONLY/REMOTE_ONLY 四类差异")
    public ApiResponse<Map<String, Object>> getReconciliationDetails(
            @Parameter(description = "对帐记录ID") @PathVariable Long reconRecordId) {
        List<ReconciliationDetail> details = reconciliationService.getReconciliationDetails(reconRecordId);

        long matchCount = details.stream().filter(d -> "MATCH".equals(d.getDiffType())).count();
        long mismatchCount = details.stream().filter(d -> "MISMATCH".equals(d.getDiffType())).count();
        long localOnlyCount = details.stream().filter(d -> "LOCAL_ONLY".equals(d.getDiffType())).count();
        long remoteOnlyCount = details.stream().filter(d -> "REMOTE_ONLY".equals(d.getDiffType())).count();

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("details", details);
        result.put("summary", Map.of(
                "total", details.size(),
                "match", matchCount,
                "mismatch", mismatchCount,
                "localOnly", localOnlyCount,
                "remoteOnly", remoteOnlyCount
        ));
        return ApiResponse.ok(result);
    }

    @GetMapping("/stats")
    @Operation(summary = "对帐统计", description = "按日期范围统计对帐结果")
    public ApiResponse<Map<String, Object>> getReconciliationStats(
            @Parameter(description = "开始日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("查询对帐统计 - 开始: {}, 结束: {}", startDate, endDate);
        Map<String, Object> stats = reconciliationService.getReconciliationStats(startDate, endDate);
        return ApiResponse.ok(stats);
    }

    @GetMapping("/health")
    @Operation(summary = "对帐服务健康检查")
    public ApiResponse<Map<String, Object>> healthCheck() {
        return ApiResponse.ok(Map.of(
                "status", "UP",
                "service", "对帐服务",
                "schedule", "每日凌晨2:00自动对帐",
                "supportedMethods", new String[]{"ALIPAY", "WECHAT"}
        ));
    }
}
