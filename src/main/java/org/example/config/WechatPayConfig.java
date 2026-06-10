package org.example.config;

import lombok.extern.slf4j.Slf4j;
import org.example.payment.service.PaymentConfigService;
import org.example.payment.service.WechatCertService;
import org.springframework.context.annotation.Configuration;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
@Configuration
public class WechatPayConfig {

    private final PaymentConfigService configService;
    private final WechatCertService certService;

    public WechatPayConfig(PaymentConfigService configService, WechatCertService certService) {
        this.configService = configService;
        this.certService = certService;
    }

    public String getAppId() { return configService.getConfig("WECHAT").getAppId(); }
    public String getMchId() { return configService.getConfig("WECHAT").getMchId(); }
    public String getApiV3Key() { return configService.getConfig("WECHAT").getApiV3Key(); }
    public String getMchSerialNo() { return configService.getConfig("WECHAT").getMchSerialNo(); }
    public String getPrivateKeyPath() { return configService.getConfig("WECHAT").getPrivateKeyPath(); }
    public String getNotifyUrl() { return configService.getConfig("WECHAT").getNotifyUrl(); }
    public String getGatewayUrl() { return configService.getConfig("WECHAT").getGatewayUrl(); }

    public String buildAuthorization(String method, String url, String body) {
        try {
            long timestamp = System.currentTimeMillis() / 1000;
            String nonce = generateNonce();
            String message = method + "\n" + url + "\n" + timestamp + "\n" + nonce + "\n" + body + "\n";
            String signature = signWithPrivateKey(message);

            return String.format(
                    "WECHATPAY2-SHA256-RSA2048 mchid=\"%s\",nonce_str=\"%s\",signature=\"%s\",timestamp=\"%d\",serial_no=\"%s\"",
                    getMchId(), nonce, signature, timestamp, getMchSerialNo());
        } catch (Exception e) {
            log.error("微信支付签名失败", e);
            throw new RuntimeException("微信签名失败: " + e.getMessage());
        }
    }

    private String signWithPrivateKey(String message) throws Exception {
        String pkcs8Key = loadPrivateKey();
        if (pkcs8Key == null || pkcs8Key.isEmpty()) {
            log.warn("微信商户私钥未配置，使用模拟签名");
            return "SIMULATED_SIGN_" + System.currentTimeMillis();
        }
        pkcs8Key = pkcs8Key
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(pkcs8Key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey pk = keyFactory.generatePrivate(spec);

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(pk);
        signature.update(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    private String loadPrivateKey() {
        String privateKeyPath = getPrivateKeyPath();
        if (privateKeyPath != null && !privateKeyPath.isEmpty()) {
            try {
                return new String(java.nio.file.Files.readAllBytes(
                        java.nio.file.Path.of(privateKeyPath)), StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.error("加载微信私钥失败: {}", privateKeyPath, e);
            }
        }
        return "";
    }

    public String hmacSha256(String data) {
        String apiV3Key = getApiV3Key();
        if (apiV3Key == null || apiV3Key.isEmpty()) {
            log.warn("微信APIv3密钥未配置，使用模拟HMAC");
            return "SIMULATED_HMAC_" + System.currentTimeMillis();
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec spec = new SecretKeySpec(apiV3Key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(spec);
            return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            log.error("HMAC-SHA256计算失败", e);
            throw new RuntimeException("HMAC计算失败: " + e.getMessage());
        }
    }

    private String generateNonce() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }

    /**
     * 验证微信支付回调签名
     * @param message 待验签消息: timestamp + "\n" + nonce + "\n" + body + "\n"
     * @param wechatSignature Base64 编码的回调签名
     * @param serial 证书序列号
     * @return 验签是否通过
     */
    public boolean verifySignature(String message, String wechatSignature, String serial) {
        return certService.verifySignature(message, wechatSignature, serial);
    }
}
