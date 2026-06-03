package org.example.payment.controller;

import org.example.entity.PaymentConfig;
import org.example.payment.service.PaymentConfigService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payment/config")
public class PaymentConfigController {

    private final PaymentConfigService configService;

    public PaymentConfigController(PaymentConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    public Map<String, Object> list() {
        List<Map<String, Object>> list = configService.listAll().stream()
                .map(this::toMaskedMap)
                .toList();
        return Map.of("success", true, "data", list, "total", list.size(),
                "timestamp", System.currentTimeMillis());
    }

    @GetMapping("/{method}")
    public Map<String, Object> get(@PathVariable String method) {
        PaymentConfig cfg = configService.getConfig(method);
        return Map.of("success", true, "data", toMaskedMap(cfg),
                "timestamp", System.currentTimeMillis());
    }

    @PutMapping("/{method}")
    public Map<String, Object> update(@PathVariable String method, @RequestBody PaymentConfig dto) {
        PaymentConfig updated = configService.updateConfig(method, dto);
        return Map.of("success", true, "data", toMaskedMap(updated),
                "message", "配置已更新，实时生效",
                "timestamp", System.currentTimeMillis());
    }

    @PostMapping("/refresh")
    public Map<String, Object> refreshAll() {
        configService.refreshAll();
        return Map.of("success", true, "message", "支付配置已刷新",
                "timestamp", System.currentTimeMillis());
    }

    private Map<String, Object> toMaskedMap(PaymentConfig cfg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", cfg.getId());
        m.put("paymentMethod", cfg.getPaymentMethod());
        m.put("appId", cfg.getAppId());
        m.put("gatewayUrl", cfg.getGatewayUrl());
        m.put("notifyUrl", cfg.getNotifyUrl());
        m.put("signType", cfg.getSignType());
        m.put("privateKey", configService.maskSensitive(cfg.getPrivateKey()));
        m.put("alipayPublicKey", configService.maskSensitive(cfg.getAlipayPublicKey()));
        m.put("returnUrl", cfg.getReturnUrl());
        m.put("mchId", cfg.getMchId());
        m.put("apiV3Key", configService.maskSensitive(cfg.getApiV3Key()));
        m.put("mchSerialNo", cfg.getMchSerialNo());
        m.put("privateKeyPath", cfg.getPrivateKeyPath());
        m.put("enabled", cfg.getEnabled());
        m.put("orderExpireMinutes", cfg.getOrderExpireMinutes() != null ? cfg.getOrderExpireMinutes() : 15);
        m.put("createTime", cfg.getCreateTime());
        m.put("updateTime", cfg.getUpdateTime());
        return m;
    }
}
