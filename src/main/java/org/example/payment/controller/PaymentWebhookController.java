package org.example.payment.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.dto.ApiResponse;
import org.example.entity.PaymentWebhook;
import org.example.mapper.PaymentWebhookMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payment/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhook 管理", description = "支付事件 Webhook 注册、删除、重试")
public class PaymentWebhookController {

    private final PaymentWebhookMapper webhookMapper;

    @GetMapping
    @Operation(summary = "列出所有 Webhook")
    public ApiResponse<List<PaymentWebhook>> list() {
        List<PaymentWebhook> list = webhookMapper.selectList(null);
        return ApiResponse.ok(list);
    }

    @PostMapping
    @Operation(summary = "注册 Webhook", description = "注册支付事件的 Webhook 回调地址")
    public ApiResponse<PaymentWebhook> create(@RequestBody PaymentWebhook webhook) {
        webhook.setId(null);
        webhook.setRetryCount(0);
        webhook.setEnabled(webhook.getEnabled() != null ? webhook.getEnabled() : true);
        webhook.setMaxRetries(webhook.getMaxRetries() != null ? webhook.getMaxRetries() : 3);
        webhookMapper.insert(webhook);
        return ApiResponse.ok(webhook, "Webhook 注册成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除 Webhook")
    public ApiResponse<Void> delete(
            @Parameter(description = "Webhook ID") @PathVariable Long id) {
        webhookMapper.deleteById(id);
        return ApiResponse.ok(null, "Webhook 已删除");
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "重置 Webhook 重试", description = "将 Webhook 的重试计数归零，下次事件时重新尝试")
    public ApiResponse<Void> retry(
            @Parameter(description = "Webhook ID") @PathVariable Long id) {
        PaymentWebhook webhook = webhookMapper.selectById(id);
        if (webhook == null) {
            return ApiResponse.fail("Webhook 不存在");
        }
        webhook.setRetryCount(0);
        webhook.setLastStatus(null);
        webhookMapper.updateById(webhook);
        return ApiResponse.ok(null, "Webhook 已重置，将在下次事件时重试");
    }
}
