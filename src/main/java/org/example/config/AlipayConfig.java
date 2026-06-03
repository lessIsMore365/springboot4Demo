package org.example.config;

import lombok.extern.slf4j.Slf4j;
import org.example.payment.service.PaymentConfigService;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Configuration
public class AlipayConfig {

    private final PaymentConfigService configService;

    public AlipayConfig(PaymentConfigService configService) {
        this.configService = configService;
    }

    public String getAppId() { return configService.getConfig("ALIPAY").getAppId(); }
    public String getPrivateKey() { return configService.getConfig("ALIPAY").getPrivateKey(); }
    public String getAlipayPublicKey() { return configService.getConfig("ALIPAY").getAlipayPublicKey(); }
    public String getGatewayUrl() { return configService.getConfig("ALIPAY").getGatewayUrl(); }
    public String getSignType() { return configService.getConfig("ALIPAY").getSignType(); }
    public String getNotifyUrl() { return configService.getConfig("ALIPAY").getNotifyUrl(); }
    public String getReturnUrl() { return configService.getConfig("ALIPAY").getReturnUrl(); }

    public String buildSignContent(Map<String, String> params) {
        TreeMap<String, String> sortedParams = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            String value = entry.getValue();
            if (value == null || value.isEmpty() ||
                    "sign".equals(entry.getKey()) || "sign_type".equals(entry.getKey())) {
                continue;
            }
            if (sb.length() > 0) sb.append("&");
            sb.append(entry.getKey()).append("=").append(value);
        }
        return sb.toString();
    }

    public String sign(String content) {
        String privateKey = getPrivateKey();
        if (privateKey == null || privateKey.isEmpty()) {
            log.warn("支付宝私钥未配置，使用模拟签名");
            return "SIMULATED_SIGN_" + System.currentTimeMillis();
        }
        try {
            String pkcs8Key = privateKey
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(pkcs8Key);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey pk = keyFactory.generatePrivate(spec);

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(pk);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception e) {
            log.error("支付宝签名失败", e);
            throw new RuntimeException("签名失败: " + e.getMessage());
        }
    }

    public boolean verify(String content, String sign) {
        String alipayPublicKey = getAlipayPublicKey();
        if (alipayPublicKey == null || alipayPublicKey.isEmpty()) {
            log.warn("支付宝公钥未配置，跳过验签");
            return true;
        }
        try {
            String pubKey = alipayPublicKey
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(pubKey);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey pk = keyFactory.generatePublic(spec);

            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(pk);
            signature.update(content.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(sign));
        } catch (Exception e) {
            log.error("支付宝验签失败", e);
            return false;
        }
    }
}
