package org.example.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.dto.ApiResponse;
import org.example.entity.PaymentConfig;
import org.example.payment.service.PaymentConfigService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payment/config")
@Tag(name = "支付配置", description = "支付宝/微信支付参数管理，修改后实时生效无需重启")
public class PaymentConfigController {

    private final PaymentConfigService configService;

    public PaymentConfigController(PaymentConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    @Operation(summary = "列出所有支付配置", description = "私钥等敏感字段自动脱敏")
    public ApiResponse<List<Map<String, Object>>> list() {
        List<Map<String, Object>> list = configService.listAll().stream()
                .map(this::toMaskedMap)
                .toList();
        return ApiResponse.ok(list);
    }

    @GetMapping("/{method}")
    @Operation(summary = "查看单个支付配置详情")
    public ApiResponse<Map<String, Object>> get(
            @Parameter(description = "支付方式: ALIPAY / WECHAT") @PathVariable String method) {
        PaymentConfig cfg = configService.getConfig(method);
        return ApiResponse.ok(toMaskedMap(cfg));
    }

    @PutMapping("/{method}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "更新支付配置", description = "仅需传要修改的字段，更新后实时生效")
    public ApiResponse<Map<String, Object>> update(
            @Parameter(description = "支付方式: ALIPAY / WECHAT") @PathVariable String method,
            @RequestBody PaymentConfig dto) {
        PaymentConfig updated = configService.updateConfig(method, dto);
        return ApiResponse.ok(toMaskedMap(updated), "配置已更新，实时生效");
    }

    @PostMapping("/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "刷新全部支付配置", description = "从数据库重新加载全部支付配置到内存缓存")
    public ApiResponse<Void> refreshAll() {
        configService.refreshAll();
        return ApiResponse.ok(null, "支付配置已刷新");
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
        m.put("wechatPlatformCert", configService.maskSensitive(cfg.getWechatPlatformCert()));
        m.put("wechatPlatformCertSerial", cfg.getWechatPlatformCertSerial());
        m.put("enabled", cfg.getEnabled());
        m.put("orderExpireMinutes", cfg.getOrderExpireMinutes() != null ? cfg.getOrderExpireMinutes() : 15);
        m.put("createTime", cfg.getCreateTime());
        m.put("updateTime", cfg.getUpdateTime());
        return m;
    }
}
