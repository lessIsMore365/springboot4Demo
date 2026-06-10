package org.example.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.PaymentWebhook;
import org.example.mapper.PaymentWebhookMapper;
import org.example.payment.event.PaymentSuccessEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Webhook 通知监听器 — 支付成功后异步通知注册的下游系统
 * 使用 HMAC-SHA256 签名，下游可验证消息完整性
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookNotificationListener {

    private final PaymentWebhookMapper webhookMapper;
    private final ObjectMapper objectMapper;

    @Async("taskExecutor")
    @EventListener
    public void onPaymentSuccess(PaymentSuccessEvent event) {
        Map<String, Object> eventData = event.toEventData();
        List<PaymentWebhook> webhooks = getEnabledWebhooks("PAYMENT.SUCCESS");

        for (PaymentWebhook webhook : webhooks) {
            deliverWebhook(webhook, eventData);
        }
    }

    private List<PaymentWebhook> getEnabledWebhooks(String eventType) {
        LambdaQueryWrapper<PaymentWebhook> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentWebhook::getEnabled, true);
        List<PaymentWebhook> all = webhookMapper.selectList(wrapper);
        return all.stream()
                .filter(w -> w.getEventTypes() != null &&
                        Arrays.asList(w.getEventTypes().split(",")).contains(eventType))
                .toList();
    }

    private void deliverWebhook(PaymentWebhook webhook, Map<String, Object> eventData) {
        try {
            String body = objectMapper.writeValueAsString(eventData);
            String signature = hmacSign(body, webhook.getSecret());

            String response = RestClient.create()
                    .post().uri(webhook.getWebhookUrl())
                    .header("Content-Type", "application/json")
                    .header("X-Payment-Signature", signature)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            webhook.setLastStatus("SUCCESS");
            webhook.setRetryCount(0);
            log.info("Webhook 通知成功 — url={}, orderNo={}", webhook.getWebhookUrl(),
                    eventData.get("orderNo"));
        } catch (Exception e) {
            webhook.setRetryCount(webhook.getRetryCount() != null ? webhook.getRetryCount() + 1 : 1);
            webhook.setLastStatus("FAILED");
            log.error("Webhook 通知失败 — url={}, retry={}", webhook.getWebhookUrl(),
                    webhook.getRetryCount(), e);
        }
        webhook.setLastCalledAt(LocalDateTime.now());
        webhookMapper.updateById(webhook);
    }

    private String hmacSign(String data, String secret) {
        if (secret == null || secret.isBlank()) return "UNSIGNED";
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec spec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(spec);
            return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return "SIGN_ERROR";
        }
    }
}
