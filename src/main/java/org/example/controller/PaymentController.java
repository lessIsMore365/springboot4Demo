package org.example.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.annotation.Log;
import org.example.entity.PaymentNotifyLog;
import org.example.entity.PaymentOrder;
import org.example.service.PaymentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 创建支付订单
     */
    @Log(title = "创建支付订单", businessType = Log.BusinessType.INSERT)
    @PostMapping("/create")
    public Map<String, Object> createPayment(@RequestBody Map<String, Object> request) {
        String validationError = validatePaymentRequest(request);
        if (validationError != null) {
            return Map.of("success", false, "message", validationError, "timestamp", System.currentTimeMillis());
        }

        String subject = request.get("subject").toString();
        String body = request.getOrDefault("body", "").toString();
        BigDecimal amount = new BigDecimal(request.get("amount").toString());
        String paymentMethod = request.get("paymentMethod").toString();
        String tradeType = request.getOrDefault("tradeType", "PAGE").toString();
        String bizType = request.getOrDefault("bizType", "").toString();
        String remark = request.getOrDefault("remark", "").toString();

        log.info("创建支付 - 方式: {}, 交易类型: {}, 业务: {}, 金额: {}", paymentMethod, tradeType, bizType, amount);
        try {
            Map<String, Object> result = paymentService.createPayment(subject, body, amount, paymentMethod, tradeType, bizType, remark);
            return Map.of("success", true, "data", result, "timestamp", System.currentTimeMillis());
        } catch (RuntimeException e) {
            log.error("创建支付订单失败 - {}", e.getMessage());
            return Map.of("success", false, "message", e.getMessage(), "timestamp", System.currentTimeMillis());
        }
    }

    /**
     * 异步创建支付订单 - 虚拟线程
     */
    @PostMapping("/create/async")
    public CompletableFuture<Map<String, Object>> createPaymentAsync(@RequestBody Map<String, Object> request) {
        String validationError = validatePaymentRequest(request);
        if (validationError != null) {
            return CompletableFuture.completedFuture(
                    Map.of("success", false, "message", validationError, "timestamp", System.currentTimeMillis()));
        }

        String subject = request.get("subject").toString();
        String body = request.getOrDefault("body", "").toString();
        BigDecimal amount = new BigDecimal(request.get("amount").toString());
        String paymentMethod = request.get("paymentMethod").toString();
        String tradeType = request.getOrDefault("tradeType", "PAGE").toString();
        String bizType = request.getOrDefault("bizType", "").toString();
        String remark = request.getOrDefault("remark", "").toString();

        log.info("异步创建支付 - 方式: {}, 交易类型: {}, 业务: {}, 金额: {}", paymentMethod, tradeType, bizType, amount);
        return paymentService.createPaymentAsync(subject, body, amount, paymentMethod, tradeType, bizType, remark)
                .thenApply(result -> Map.of("success", true, "data", result, "timestamp", System.currentTimeMillis()));
    }

    /**
     * 支付宝异步通知回调（公开接口）
     */
    @PostMapping("/notify/alipay")
    public String alipayNotify(@RequestParam Map<String, String> params) {
        log.info("支付宝支付异步通知");
        return paymentService.handleAlipayNotify(params);
    }

    /**
     * 微信支付异步通知回调（公开接口）
     */
    @PostMapping("/notify/wechat")
    public Map<String, Object> wechatNotify(@RequestBody String body,
                                             @RequestHeader("Wechatpay-Signature") String signature,
                                             @RequestHeader("Wechatpay-Serial") String serial,
                                             @RequestHeader("Wechatpay-Nonce") String nonce,
                                             @RequestHeader("Wechatpay-Timestamp") String timestamp) {
        log.info("微信支付异步通知");
        return paymentService.handleWechatNotify(body, signature, serial, nonce, timestamp);
    }

    /**
     * 查询订单状态
     */
    @GetMapping("/order/{orderNo}")
    public Map<String, Object> queryOrder(@PathVariable String orderNo) {
        PaymentOrder order = paymentService.queryOrder(orderNo);
        if (order == null) {
            return Map.of("success", false, "message", "订单不存在", "timestamp", System.currentTimeMillis());
        }
        return Map.of("success", true, "data", order, "timestamp", System.currentTimeMillis());
    }

    /**
     * 关闭订单
     */
    @PostMapping("/order/{orderNo}/close")
    public Map<String, Object> closeOrder(@PathVariable String orderNo) {
        boolean closed = paymentService.closeOrder(orderNo);
        return Map.of(
                "success", closed,
                "message", closed ? "订单已关闭" : "订单关闭失败（可能不存在或状态不允许）",
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 申请退款
     */
    @Log(title = "申请退款", businessType = Log.BusinessType.UPDATE)
    @PostMapping("/refund")
    public Map<String, Object> refund(@RequestBody Map<String, Object> request) {
        Object orderNoObj = request.get("orderNo");
        Object amountObj = request.get("amount");

        if (orderNoObj == null || orderNoObj.toString().isBlank()) {
            return Map.of("success", false, "message", "订单号不能为空", "timestamp", System.currentTimeMillis());
        }
        if (amountObj == null) {
            return Map.of("success", false, "message", "退款金额不能为空", "timestamp", System.currentTimeMillis());
        }

        String orderNo = orderNoObj.toString();
        BigDecimal amount;
        try {
            amount = new BigDecimal(amountObj.toString());
        } catch (NumberFormatException e) {
            return Map.of("success", false, "message", "退款金额格式无效", "timestamp", System.currentTimeMillis());
        }
        String reason = request.getOrDefault("reason", "用户申请退款").toString();

        log.info("申请退款 - 订单号: {}, 金额: {}", orderNo, amount);
        try {
            Map<String, Object> result = paymentService.refund(orderNo, amount, reason);
            return Map.of("success", true, "data", result, "timestamp", System.currentTimeMillis());
        } catch (RuntimeException e) {
            log.error("退款失败 - {}", e.getMessage());
            return Map.of("success", false, "message", e.getMessage(), "timestamp", System.currentTimeMillis());
        }
    }

    /**
     * 分页查询支付订单
     */
    @GetMapping("/orders")
    public Map<String, Object> getOrders(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        Page<PaymentOrder> orderPage = paymentService.getOrdersByPage(page, size);
        return Map.of(
                "success", true,
                "data", orderPage.getRecords(),
                "pagination", Map.of(
                        "page", orderPage.getCurrent(),
                        "size", orderPage.getSize(),
                        "total", orderPage.getTotal(),
                        "pages", orderPage.getPages()
                ),
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 分页查询回调通知日志
     */
    @GetMapping("/notify-logs")
    public Map<String, Object> getNotifyLogs(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) String paymentMethod,
                                              @RequestParam(required = false) String orderNo) {
        Page<PaymentNotifyLog> logPage = paymentService.getNotifyLogsByPage(page, size, paymentMethod, orderNo);
        return Map.of(
                "success", true,
                "data", logPage.getRecords(),
                "pagination", Map.of(
                        "page", logPage.getCurrent(),
                        "size", logPage.getSize(),
                        "total", logPage.getTotal(),
                        "pages", logPage.getPages()
                ),
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 查询单条回调日志详情
     */
    @GetMapping("/notify-log/{id}")
    public Map<String, Object> getNotifyLog(@PathVariable Long id) {
        PaymentNotifyLog logEntry = paymentService.getNotifyLogById(id);
        if (logEntry == null) {
            return Map.of("success", false, "message", "日志不存在", "timestamp", System.currentTimeMillis());
        }
        return Map.of("success", true, "data", logEntry, "timestamp", System.currentTimeMillis());
    }

    /**
     * 清理旧回调日志
     */
    @DeleteMapping("/notify-logs")
    public Map<String, Object> deleteOldNotifyLogs(@RequestParam(defaultValue = "90") int beforeDays) {
        int deleted = paymentService.deleteOldNotifyLogs(beforeDays);
        return Map.of(
                "success", true,
                "message", "已清理 " + beforeDays + " 天前的回调日志，共 " + deleted + " 条",
                "deletedCount", deleted,
                "timestamp", System.currentTimeMillis()
        );
    }

    private static final java.util.Set<String> VALID_METHODS = java.util.Set.of("ALIPAY", "WECHAT");
    private static final java.util.Set<String> VALID_TRADE_TYPES = java.util.Set.of("PAGE", "WAP", "APP", "JSAPI");

    private String validatePaymentRequest(Map<String, Object> request) {
        Object subjectObj = request.get("subject");
        Object amountObj = request.get("amount");
        Object methodObj = request.get("paymentMethod");

        if (subjectObj == null || subjectObj.toString().isBlank()) {
            return "商品标题(subject)不能为空";
        }
        if (amountObj == null) {
            return "支付金额(amount)不能为空";
        }
        try {
            BigDecimal amount = new BigDecimal(amountObj.toString());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return "支付金额必须大于0";
            }
        } catch (NumberFormatException e) {
            return "支付金额格式无效";
        }
        if (methodObj == null || methodObj.toString().isBlank()) {
            return "支付方式(paymentMethod)不能为空";
        }
        if (!VALID_METHODS.contains(methodObj.toString().toUpperCase())) {
            return "不支持的支付方式: " + methodObj + "，仅支持 ALIPAY / WECHAT";
        }
        Object tradeTypeObj = request.get("tradeType");
        if (tradeTypeObj != null && !VALID_TRADE_TYPES.contains(tradeTypeObj.toString().toUpperCase())) {
            return "不支持的交易类型: " + tradeTypeObj + "，仅支持 PAGE / WAP / APP / JSAPI";
        }
        return null;
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        return Map.of(
                "status", "UP",
                "service", "支付服务",
                "supportedMethods", new String[]{"ALIPAY", "WECHAT"},
                "timestamp", System.currentTimeMillis()
        );
    }
}
