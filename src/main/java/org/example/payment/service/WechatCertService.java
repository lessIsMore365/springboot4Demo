package org.example.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 微信支付平台证书管理 — 定时拉取并缓存平台证书用于验签
 * 微信平台证书每 6-12 个月轮换，定时刷新确保验签可用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatCertService {

    private final PaymentConfigService configService;
    private final ConcurrentHashMap<String, PublicKey> certCache = new ConcurrentHashMap<>();

    /**
     * 验签微信支付回调签名
     * @param message 待验签消息: timestamp + "\n" + nonce + "\n" + body + "\n"
     * @param signature Base64 编码的签名
     * @param serial 证书序列号
     * @return 验签是否通过
     */
    public boolean verifySignature(String message, String signature, String serial) {
        try {
            PublicKey publicKey = getPlatformCert(serial);
            if (publicKey == null) {
                log.warn("微信平台证书不存在 — serial={}", serial);
                return false;
            }
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(message.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return sig.verify(Base64.getDecoder().decode(signature));
        } catch (Exception e) {
            log.error("微信验签异常 — serial={}", serial, e);
            return false;
        }
    }

    private PublicKey getPlatformCert(String serial) {
        // 先从缓存取
        PublicKey cached = certCache.get(serial);
        if (cached != null) return cached;

        // 从数据库获取
        String certPem = configService.getConfig("WECHAT").getWechatPlatformCert();
        if (certPem == null || certPem.isBlank()) {
            log.warn("微信平台证书未配置 — 请设置 wechatPlatformCert");
            return null;
        }
        try {
            PublicKey pk = parsePublicKey(certPem);
            certCache.put(serial, pk);
            return pk;
        } catch (Exception e) {
            log.error("解析微信平台证书失败", e);
            return null;
        }
    }

    private PublicKey parsePublicKey(String certPem) throws Exception {
        String key = certPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }

    /**
     * 每 12 小时从数据库刷新证书缓存
     * 生产环境应调用 GET /v3/certificates 接口获取最新平台证书
     */
    @Scheduled(fixedRate = 43200000)
    public void refreshCerts() {
        log.info("刷新微信平台证书缓存...");
        certCache.clear();
    }
}
