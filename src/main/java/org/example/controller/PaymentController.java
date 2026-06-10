package org.example.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.annotation.Idempotent;
import org.example.annotation.Log;
import org.example.annotation.RateLimit;
import org.example.dto.ApiResponse;
import org.example.dto.PageResponse;
import org.example.dto.payment.CloseOrderRequest;
import org.example.dto.payment.CreatePaymentRequest;
import org.example.dto.payment.RefundRequest;
import org.example.entity.PaymentNotifyLog;
import org.example.entity.PaymentOrder;
import org.example.service.PaymentService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Tag(name = "支付管理", description = "创建支付订单、查询订单状态、退款、关闭订单、回调通知处理")
public class PaymentController {

    private final PaymentService paymentService;

    @Log(title = "创建支付订单", businessType = Log.BusinessType.INSERT)
    @Idempotent(prefix = "payment:create")
    @RateLimit(key = "payment_create", permitsPerSecond = 10)
    @PostMapping("/create")
    @Operation(summary = "创建支付订单", description = "创建支付宝/微信支付订单，支持 PAGE/WAP/APP/JSAPI 四种交易类型")
    public ApiResponse<Map<String, Object>> createPayment(@Valid @RequestBody CreatePaymentRequest req) {
        log.info("创建支付 - 方式: {}, 交易类型: {}, 业务: {}, 金额: {}", req.paymentMethod(), req.tradeType(), req.bizType(), req.amount());
        Map<String, Object> result = paymentService.createPayment(
                req.subject(), bodyOrEmpty(req.body()), req.amount(),
                req.paymentMethod(), tradeTypeOrPage(req.tradeType()),
                req.bizType(), req.remark());
        return ApiResponse.ok(result);
    }

    @PostMapping("/create/async")
    @Operation(summary = "异步创建支付订单", description = "通过虚拟线程异步创建支付订单")
    public CompletableFuture<ApiResponse<Map<String, Object>>> createPaymentAsync(@Valid @RequestBody CreatePaymentRequest req) {
        log.info("异步创建支付 - 方式: {}, 交易类型: {}, 业务: {}, 金额: {}", req.paymentMethod(), req.tradeType(), req.bizType(), req.amount());
        return paymentService.createPaymentAsync(
                        req.subject(), bodyOrEmpty(req.body()), req.amount(),
                        req.paymentMethod(), tradeTypeOrPage(req.tradeType()),
                        req.bizType(), req.remark())
                .thenApply(ApiResponse::ok);
    }

    @RateLimit(key = "payment_notify_alipay", permitsPerSecond = 50)
    @PostMapping("/notify/alipay")
    @Operation(summary = "支付宝异步通知回调", description = "支付宝服务器异步通知支付结果，返回 success 或 failure")
    public String alipayNotify(@RequestParam Map<String, String> params) {
        log.info("支付宝支付异步通知");
        return paymentService.handleAlipayNotify(params);
    }

    @RateLimit(key = "payment_notify_wechat", permitsPerSecond = 50)
    @PostMapping("/notify/wechat")
    @Operation(summary = "微信支付异步通知回调", description = "微信服务器异步通知支付结果，需验证签名")
    public Map<String, Object> wechatNotify(@RequestBody String body,
                                             @RequestHeader("Wechatpay-Signature") String signature,
                                             @RequestHeader("Wechatpay-Serial") String serial,
                                             @RequestHeader("Wechatpay-Nonce") String nonce,
                                             @RequestHeader("Wechatpay-Timestamp") String timestamp) {
        log.info("微信支付异步通知");
        return paymentService.handleWechatNotify(body, signature, serial, nonce, timestamp);
    }

    @GetMapping("/order/{orderNo}")
    @Operation(summary = "查询订单状态", description = "根据商户订单号查询支付订单详情")
    public ApiResponse<?> queryOrder(
            @Parameter(description = "商户订单号") @PathVariable String orderNo) {
        PaymentOrder order = paymentService.queryOrder(orderNo);
        if (order == null) {
            return ApiResponse.fail("订单不存在");
        }
        return ApiResponse.ok(order);
    }

    @Idempotent(prefix = "payment:close")
    @RateLimit(key = "payment_close", permitsPerSecond = 5)
    @PostMapping("/order/{orderNo}/close")
    @Operation(summary = "关闭订单", description = "关闭未支付的订单")
    public ApiResponse<Void> closeOrder(
            @Parameter(description = "商户订单号") @PathVariable String orderNo) {
        boolean closed = paymentService.closeOrder(orderNo);
        return ApiResponse.of(closed, closed ? "订单已关闭" : "订单关闭失败（可能不存在或状态不允许）");
    }

    @Log(title = "申请退款", businessType = Log.BusinessType.UPDATE)
    @Idempotent(prefix = "payment:refund")
    @RateLimit(key = "payment_refund", permitsPerSecond = 5)
    @PostMapping("/refund")
    @Operation(summary = "申请退款", description = "对已支付订单发起退款，支持部分退款")
    public ApiResponse<Map<String, Object>> refund(@Valid @RequestBody RefundRequest req) {
        log.info("申请退款 - 订单号: {}, 金额: {}", req.orderNo(), req.amount());
        Map<String, Object> result = paymentService.refund(req.orderNo(), req.amount(),
                req.reason() != null ? req.reason() : "用户申请退款");
        return ApiResponse.ok(result);
    }

    @GetMapping("/orders")
    @Operation(summary = "分页查询支付订单")
    public ApiResponse<PageResponse<PaymentOrder>> getOrders(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        Page<PaymentOrder> orderPage = paymentService.getOrdersByPage(page, size);
        return ApiResponse.ok(PageResponse.of(orderPage.getRecords(), orderPage.getCurrent(),
                orderPage.getSize(), orderPage.getTotal()));
    }

    @GetMapping("/notify-logs")
    @Operation(summary = "分页查询回调通知日志", description = "查询支付宝/微信支付回调通知日志，支持按支付方式和订单号筛选")
    public ApiResponse<PageResponse<PaymentNotifyLog>> getNotifyLogs(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "支付方式: ALIPAY / WECHAT") @RequestParam(required = false) String paymentMethod,
            @Parameter(description = "商户订单号") @RequestParam(required = false) String orderNo) {
        Page<PaymentNotifyLog> logPage = paymentService.getNotifyLogsByPage(page, size, paymentMethod, orderNo);
        return ApiResponse.ok(PageResponse.of(logPage.getRecords(), logPage.getCurrent(),
                logPage.getSize(), logPage.getTotal()));
    }

    @GetMapping("/notify-log/{id}")
    @Operation(summary = "查询单条回调日志详情")
    public ApiResponse<?> getNotifyLog(
            @Parameter(description = "日志ID") @PathVariable Long id) {
        PaymentNotifyLog logEntry = paymentService.getNotifyLogById(id);
        if (logEntry == null) {
            return ApiResponse.fail("日志不存在");
        }
        return ApiResponse.ok(logEntry);
    }

    @DeleteMapping("/notify-logs")
    @Operation(summary = "清理旧回调日志", description = "删除指定天数之前的回调通知日志")
    public ApiResponse<Void> deleteOldNotifyLogs(
            @Parameter(description = "清理多少天前的日志") @RequestParam(defaultValue = "90") int beforeDays) {
        int deleted = paymentService.deleteOldNotifyLogs(beforeDays);
        return ApiResponse.ok(null, "已清理 " + beforeDays + " 天前的回调日志，共 " + deleted + " 条");
    }

    @GetMapping("/health")
    @Operation(summary = "支付服务健康检查")
    public ApiResponse<Map<String, Object>> healthCheck() {
        return ApiResponse.ok(Map.of(
                "status", "UP",
                "service", "支付服务",
                "supportedMethods", new String[]{"ALIPAY", "WECHAT"}
        ));
    }

    private static String bodyOrEmpty(String body) {
        return body != null ? body : "";
    }

    private static String tradeTypeOrPage(String tradeType) {
        return tradeType != null ? tradeType : "PAGE";
    }
}
