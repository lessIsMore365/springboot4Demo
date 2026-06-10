package org.example.payment.service;

import org.example.entity.PaymentConfig;
import org.example.mapper.PaymentConfigMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PaymentConfigService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PaymentConfigService.class);

    private final PaymentConfigMapper mapper;
    private final Map<String, PaymentConfig> configCache = new ConcurrentHashMap<>();

    public PaymentConfigService(PaymentConfigMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedDefaults();
        loadFromDb();
    }

    // ========== 缓存加载 ==========

    public void loadFromDb() {
        List<PaymentConfig> list = mapper.selectList(null);
        configCache.clear();
        for (PaymentConfig cfg : list) {
            configCache.put(cfg.getPaymentMethod(), cfg);
        }
        log.info("Loaded {} payment configs from database", list.size());
    }

    public void seedDefaults() {
        if (mapper.selectCount(null) > 0) return;

        PaymentConfig alipay = new PaymentConfig();
        alipay.setPaymentMethod("ALIPAY");
        alipay.setAppId("2021000000000000");
        alipay.setGatewayUrl("https://openapi.alipay.com/gateway.do");
        alipay.setSignType("RSA2");
        alipay.setNotifyUrl("http://localhost:8080/api/payment/notify/alipay");
        alipay.setReturnUrl("http://localhost:8080/payment/result");
        alipay.setPrivateKey("");
        alipay.setAlipayPublicKey("");
        alipay.setEnabled(true);
        alipay.setOrderExpireMinutes(15);
        mapper.insert(alipay);

        PaymentConfig wechat = new PaymentConfig();
        wechat.setPaymentMethod("WECHAT");
        wechat.setAppId("wx0000000000000000");
        wechat.setGatewayUrl("https://api.mch.weixin.qq.com");
        wechat.setNotifyUrl("http://localhost:8080/api/payment/notify/wechat");
        wechat.setMchId("0000000000");
        wechat.setApiV3Key("");
        wechat.setMchSerialNo("");
        wechat.setPrivateKeyPath("");
        wechat.setEnabled(true);
        wechat.setOrderExpireMinutes(15);
        mapper.insert(wechat);

        log.info("Seeded default payment configs (ALIPAY, WECHAT)");
    }

    // ========== 查询 ==========

    public PaymentConfig getConfig(String paymentMethod) {
        PaymentConfig cfg = configCache.get(paymentMethod.toUpperCase());
        if (cfg == null) {
            throw new IllegalArgumentException("未知的支付方式: " + paymentMethod);
        }
        return cfg;
    }

    public List<PaymentConfig> listAll() {
        return configCache.values().stream()
                .sorted(Comparator.comparing(PaymentConfig::getPaymentMethod))
                .toList();
    }

    // ========== 更新 ==========

    public PaymentConfig updateConfig(String paymentMethod, PaymentConfig dto) {
        PaymentConfig existing = configCache.get(paymentMethod.toUpperCase());
        if (existing == null) {
            throw new IllegalArgumentException("未知的支付方式: " + paymentMethod);
        }

        if (dto.getAppId() != null) existing.setAppId(dto.getAppId());
        if (dto.getGatewayUrl() != null) existing.setGatewayUrl(dto.getGatewayUrl());
        if (dto.getNotifyUrl() != null) existing.setNotifyUrl(dto.getNotifyUrl());
        if (dto.getSignType() != null) existing.setSignType(dto.getSignType());
        if (dto.getPrivateKey() != null) existing.setPrivateKey(dto.getPrivateKey());
        if (dto.getAlipayPublicKey() != null) existing.setAlipayPublicKey(dto.getAlipayPublicKey());
        if (dto.getReturnUrl() != null) existing.setReturnUrl(dto.getReturnUrl());
        if (dto.getMchId() != null) existing.setMchId(dto.getMchId());
        if (dto.getApiV3Key() != null) existing.setApiV3Key(dto.getApiV3Key());
        if (dto.getMchSerialNo() != null) existing.setMchSerialNo(dto.getMchSerialNo());
        if (dto.getPrivateKeyPath() != null) existing.setPrivateKeyPath(dto.getPrivateKeyPath());
        if (dto.getWechatPlatformCert() != null) existing.setWechatPlatformCert(dto.getWechatPlatformCert());
        if (dto.getWechatPlatformCertSerial() != null) existing.setWechatPlatformCertSerial(dto.getWechatPlatformCertSerial());
        if (dto.getEnabled() != null) existing.setEnabled(dto.getEnabled());
        if (dto.getOrderExpireMinutes() != null) existing.setOrderExpireMinutes(dto.getOrderExpireMinutes());

        mapper.updateById(existing);
        configCache.put(paymentMethod.toUpperCase(), existing);

        log.info("Payment config updated: {} (enabled={})", paymentMethod, existing.getEnabled());
        return existing;
    }

    public void refreshAll() {
        loadFromDb();
    }

    public int getOrderExpireMinutes(String paymentMethod) {
        PaymentConfig cfg = configCache.get(paymentMethod.toUpperCase());
        if (cfg == null || cfg.getOrderExpireMinutes() == null || cfg.getOrderExpireMinutes() <= 0) {
            return 15;
        }
        return cfg.getOrderExpireMinutes();
    }

    // ========== 校验 ==========

    public String validateConfig(String paymentMethod) {
        PaymentConfig cfg = getConfig(paymentMethod);

        if (!Boolean.TRUE.equals(cfg.getEnabled())) {
            return paymentMethod.toUpperCase() + " 支付未启用，请先在支付配置中启用";
        }

        List<String> missing = new ArrayList<>();

        if (isBlank(cfg.getAppId())) missing.add("AppId");
        if (isBlank(cfg.getGatewayUrl())) missing.add("网关地址");

        if ("ALIPAY".equalsIgnoreCase(paymentMethod)) {
            if (isBlank(cfg.getPrivateKey())) missing.add("商户私钥");
            if (isBlank(cfg.getAlipayPublicKey())) missing.add("支付宝公钥");
        } else if ("WECHAT".equalsIgnoreCase(paymentMethod)) {
            if (isBlank(cfg.getMchId())) missing.add("商户号(mchId)");
            if (isBlank(cfg.getApiV3Key())) missing.add("APIv3密钥");
            if (isBlank(cfg.getMchSerialNo())) missing.add("商户证书序列号");
        }

        if (!missing.isEmpty()) {
            return "支付配置不完整，缺少: " + String.join("、", missing) + "，请在 API 文档中的支付配置接口中补充";
        }

        return null; // 校验通过
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    // ========== 脱敏 ==========

    public String maskSensitive(String value) {
        if (value == null || value.isEmpty()) return "";
        if (value.length() <= 8) return "****";
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}
