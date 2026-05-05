package org.example.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "payment.wechat")
public class WechatPayConfig {

    private String appId;
    private String mchId;
    private String apiV3Key;
    private String mchSerialNo;
    private String privateKeyPath;
    private String notifyUrl;
    private String gatewayUrl = "https://api.mch.weixin.qq.com";

    /**
     * 生成微信支付APIv3签名
     * 格式: WECHATPAY2-SHA256-RSA2048 mchid="...",nonce_str="...",signature="...",timestamp="...",serial_no="..."
     */
    public String buildAuthorization(String method, String url, String body) {
        try {
            long timestamp = System.currentTimeMillis() / 1000;
            String nonce = generateNonce();
            String message = method + "\n" + url + "\n" + timestamp + "\n" + nonce + "\n" + body + "\n";
            String signature = signWithPrivateKey(message);

            return String.format(
                    "WECHATPAY2-SHA256-RSA2048 mchid=\"%s\",nonce_str=\"%s\",signature=\"%s\",timestamp=\"%d\",serial_no=\"%s\"",
                    mchId, nonce, signature, timestamp, mchSerialNo);
        } catch (Exception e) {
            log.error("微信支付签名失败", e);
            throw new RuntimeException("微信签名失败: " + e.getMessage());
        }
    }

    /**
     * 使用商户私钥对报文签名
     */
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

    /**
     * 加载商户私钥（实际从文件路径加载）
     */
    private String loadPrivateKey() {
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

    /**
     * HMAC-SHA256 用于回调验签
     */
    public String hmacSha256(String data) {
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
}
