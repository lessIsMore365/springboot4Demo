package org.example.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.config.AlipayConfig;
import org.example.config.WechatPayConfig;
import org.example.entity.PaymentOrder;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 支付 API 客户端 — 封装对支付宝/微信网关的 HTTP 调用
 * 使用 @Retryable 在临时网络故障时自动重试
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentApiClient {

    private final AlipayConfig alipayConfig;
    private final WechatPayConfig wechatPayConfig;
    private final ObjectMapper objectMapper;

    /**
     * 调用支付宝网关 — POST 表单
     */
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2),
            retryFor = {Exception.class}, noRetryFor = {IllegalArgumentException.class})
    public String callAlipayGateway(Map<String, String> params) {
        try {
            RestClient client = RestClient.create(alipayConfig.getGatewayUrl());
            log.info("调用支付宝网关 — method={}", params.get("method"));
            // 生产环境取消注释：
            // return client.post().body(buildFormBody(params))
            //         .header("Content-Type", "application/x-www-form-urlencoded")
            //         .retrieve().body(String.class);
            return "{\"alipay_trade_pay_response\":{\"code\":\"10000\",\"msg\":\"Success\"}}";
        } catch (Exception e) {
            log.error("调用支付宝网关失败", e);
            throw new RuntimeException("支付宝网关调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 调用微信支付 API
     */
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2),
            retryFor = {Exception.class}, noRetryFor = {IllegalArgumentException.class})
    public String callWechatApi(String method, String uri, String body) {
        try {
            String auth = wechatPayConfig.buildAuthorization(method, uri, body);
            RestClient client = RestClient.create(wechatPayConfig.getGatewayUrl());
            log.info("调用微信支付API — {} {}", method, uri);
            // 生产环境取消注释：
            // return client.method(org.springframework.http.HttpMethod.valueOf(method))
            //         .uri(uri).header("Authorization", auth)
            //         .header("Accept", "application/json")
            //         .header("Content-Type", "application/json")
            //         .body(body).retrieve().body(String.class);
            return "{\"prepay_id\":\"wx_prepay_sim_" + System.currentTimeMillis() + "\"}";
        } catch (Exception e) {
            log.error("调用微信支付API失败", e);
            throw new RuntimeException("微信API调用失败: " + e.getMessage(), e);
        }
    }

    @Recover
    public String recoverFromApiFailure(Exception e, String method, String uri, String body) {
        log.error("微信支付API调用最终失败（已重试3次） — {} {}", method, uri, e);
        throw new RuntimeException("支付网关调用失败，已重试3次: " + e.getMessage(), e);
    }

    @Recover
    public String recoverFromAlipayFailure(Exception e, Map<String, String> params) {
        log.error("支付宝网关调用最终失败（已重试3次） — method={}", params.get("method"), e);
        throw new RuntimeException("支付宝网关调用失败，已重试3次: " + e.getMessage(), e);
    }
}
