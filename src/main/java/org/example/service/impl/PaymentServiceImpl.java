package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.config.AlipayConfig;
import org.example.config.WechatPayConfig;
import org.example.entity.PaymentNotifyLog;
import org.example.entity.PaymentOrder;
import org.example.mapper.PaymentNotifyLogMapper;
import org.example.mapper.PaymentOrderMapper;
import org.example.payment.service.PaymentConfigService;
import org.example.service.PaymentService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentOrderMapper paymentOrderMapper;
    private final PaymentNotifyLogMapper notifyLogMapper;
    private final PaymentConfigService paymentConfigService;
    private final AlipayConfig alipayConfig;
    private final WechatPayConfig wechatPayConfig;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest request;

    private static final DateTimeFormatter ORDER_NO_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Set<String> VALID_METHODS = Set.of("ALIPAY", "WECHAT");
    private static final Set<String> VALID_TRADE_TYPES = Set.of("PAGE", "WAP", "APP", "JSAPI");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createPayment(String subject, String body, BigDecimal amount, String paymentMethod, String tradeType, String bizType, String remark) {
        Thread currentThread = Thread.currentThread();
        log.info("创建支付订单 - 方式: {}, 金额: {}, 交易类型: {}, 线程: {}, 虚拟线程: {}",
                paymentMethod, amount, tradeType, currentThread, currentThread.isVirtual());

        if (!VALID_METHODS.contains(paymentMethod.toUpperCase())) {
            throw new RuntimeException("不支持的支付方式: " + paymentMethod + "，仅支持 ALIPAY / WECHAT");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("支付金额必须大于0");
        }

        String tt = tradeType != null ? tradeType.toUpperCase() : "PAGE";
        if (!VALID_TRADE_TYPES.contains(tt)) {
            throw new RuntimeException("不支持的交易类型: " + tradeType + "，仅支持 PAGE / WAP / APP / JSAPI");
        }

        String validationError = paymentConfigService.validateConfig(paymentMethod);
        if (validationError != null) {
            throw new RuntimeException(validationError);
        }

        String orderNo = generateOrderNo(paymentMethod);

        PaymentOrder order = new PaymentOrder();
        order.setOrderNo(orderNo);
        order.setPaymentMethod(paymentMethod.toUpperCase());
        order.setAmount(amount.setScale(2, RoundingMode.HALF_UP));
        order.setSubject(subject);
        order.setBody(body);
        order.setBizType(bizType != null && !bizType.isBlank() ? bizType : null);
        order.setRemark(remark != null && !remark.isBlank() ? remark : null);
        order.setStatus("PENDING");
        paymentOrderMapper.insert(order);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNo", orderNo);
        result.put("amount", order.getAmount());
        result.put("paymentMethod", order.getPaymentMethod());
        result.put("tradeType", tt);
        result.put("status", "PENDING");

        Map<String, Object> payData;
        if ("ALIPAY".equalsIgnoreCase(paymentMethod)) {
            payData = buildAlipayPayData(orderNo, tt);
        } else {
            payData = buildWechatPayData(orderNo, tt);
        }
        result.putAll(payData);

        // 保存支付链接到订单，方便未支付时从列表/详情继续支付
        try {
            order.setPayData(objectMapper.writeValueAsString(payData));
            paymentOrderMapper.updateById(order);
        } catch (Exception e) {
            log.error("保存支付链接失败 - 订单号: {}", orderNo, e);
        }

        return result;
    }

    // ==================== 支付宝 — 按交易类型构建支付数据 ====================

    private Map<String, Object> buildAlipayPayData(String orderNo, String tradeType) {
        PaymentOrder order = queryOrder(orderNo);
        Map<String, Object> payData = new LinkedHashMap<>();

        switch (tradeType) {
            case "PAGE" -> {
                // PC 网页支付 → HTML 表单自动跳转
                payData.put("payForm", buildAlipayForm(order, "alipay.trade.page.pay", "FAST_INSTANT_TRADE_PAY"));
            }
            case "WAP" -> {
                // 移动 H5 支付 → 重定向 URL
                String redirectUrl = buildAlipayRedirectUrl(order, "alipay.trade.wap.pay", "QUICK_WAP_WAY");
                payData.put("redirectUrl", redirectUrl);
            }
            case "APP" -> {
                // App 支付 → orderString 给 SDK 调起
                payData.put("orderString", buildAlipayOrderString(order));
            }
            case "JSAPI" -> {
                // 小程序/生活号 → tradeNo
                payData.put("tradeNo", buildAlipayTradeNo(order));
            }
        }
        return payData;
    }

    private String buildAlipayForm(PaymentOrder order, String method, String productCode) {
        Map<String, String> bizContent = new LinkedHashMap<>();
        bizContent.put("out_trade_no", order.getOrderNo());
        bizContent.put("total_amount", order.getAmount().toString());
        bizContent.put("subject", order.getSubject());
        bizContent.put("body", order.getBody() != null ? order.getBody() : "");
        bizContent.put("product_code", productCode);
        bizContent.put("timeout_express", getAlipayTimeout());

        Map<String, String> params = buildAlipayParams(method, bizContent);
        return buildAutoSubmitHtml(params);
    }

    private String buildAlipayRedirectUrl(PaymentOrder order, String method, String productCode) {
        Map<String, String> bizContent = new LinkedHashMap<>();
        bizContent.put("out_trade_no", order.getOrderNo());
        bizContent.put("total_amount", order.getAmount().toString());
        bizContent.put("subject", order.getSubject());
        bizContent.put("body", order.getBody() != null ? order.getBody() : "");
        bizContent.put("product_code", productCode);
        bizContent.put("timeout_express", getAlipayTimeout());

        Map<String, String> params = buildAlipayParams(method, bizContent);
        // 拼接 GET 重定向 URL
        StringBuilder url = new StringBuilder(alipayConfig.getGatewayUrl()).append("?");
        for (Map.Entry<String, String> e : params.entrySet()) {
            url.append(e.getKey()).append("=").append(java.net.URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8)).append("&");
        }
        return url.toString();
    }

    private String buildAlipayOrderString(PaymentOrder order) {
        Map<String, String> bizContent = new LinkedHashMap<>();
        bizContent.put("out_trade_no", order.getOrderNo());
        bizContent.put("total_amount", order.getAmount().toString());
        bizContent.put("subject", order.getSubject());
        bizContent.put("body", order.getBody() != null ? order.getBody() : "");
        bizContent.put("product_code", "QUICK_MSECURITY_PAY");
        bizContent.put("timeout_express", getAlipayTimeout());

        Map<String, String> params = buildAlipayParams("alipay.trade.app.pay", bizContent);
        // App 支付返回 key=value&key=value 格式的 orderString
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (sb.length() > 0) sb.append("&");
            sb.append(e.getKey()).append("=").append(java.net.URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private String buildAlipayTradeNo(PaymentOrder order) {
        // JSAPI 支付返回 tradeNo，前端用 tradeNo 调起支付
        log.info("支付宝 JSAPI 支付 — tradeNo 已生成，订单号: {}", order.getOrderNo());
        return "ALIPAY_TN_" + order.getOrderNo() + "_" + System.currentTimeMillis();
    }

    private Map<String, String> buildAlipayParams(String method, Map<String, String> bizContent) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("app_id", alipayConfig.getAppId());
        params.put("method", method);
        params.put("charset", "UTF-8");
        params.put("sign_type", alipayConfig.getSignType());
        params.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        params.put("version", "1.0");
        params.put("notify_url", alipayConfig.getNotifyUrl());
        if (alipayConfig.getReturnUrl() != null && !alipayConfig.getReturnUrl().isBlank()) {
            params.put("return_url", alipayConfig.getReturnUrl());
        }
        try {
            params.put("biz_content", objectMapper.writeValueAsString(bizContent));
        } catch (Exception e) {
            throw new RuntimeException("构建支付参数失败", e);
        }
        String signContent = alipayConfig.buildSignContent(params);
        params.put("sign", alipayConfig.sign(signContent));
        return params;
    }

    private String buildAutoSubmitHtml(Map<String, String> params) {
        StringBuilder html = new StringBuilder();
        html.append("<form id=\"alipayForm\" action=\"").append(alipayConfig.getGatewayUrl())
                .append("\" method=\"POST\">\n");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            html.append("  <input type=\"hidden\" name=\"").append(entry.getKey())
                    .append("\" value=\"").append(escapeHtml(entry.getValue())).append("\"/>\n");
        }
        html.append("  <script>document.getElementById('alipayForm').submit();</script>\n");
        html.append("</form>");
        return html.toString();
    }

    // ==================== 微信支付 — 按交易类型构建支付数据 ====================

    private Map<String, Object> buildWechatPayData(String orderNo, String tradeType) {
        PaymentOrder order = queryOrder(orderNo);
        Map<String, Object> payData = new LinkedHashMap<>();
        int totalCents = order.getAmount().multiply(new BigDecimal("100")).intValue();

        switch (tradeType) {
            case "PAGE" -> {
                // PC 扫码支付 → codeUrl
                payData.put("codeUrl", buildWechatNativePay(order, totalCents));
            }
            case "WAP" -> {
                // 移动 H5 支付 → h5_url
                payData.put("h5Url", buildWechatH5Pay(order, totalCents));
            }
            case "APP" -> {
                // App 支付 → prepay_id + 签名参数
                payData.putAll(buildWechatAppPay(order, totalCents));
            }
            case "JSAPI" -> {
                // 小程序/公众号 → prepay_id + 签名参数
                payData.putAll(buildWechatJsapiPay(order, totalCents));
            }
        }
        return payData;
    }

    private String buildWechatNativePay(PaymentOrder order, int totalCents) {
        String prepayId = createWechatPrepayId(order, totalCents, "NATIVE");
        String simulatedCodeUrl = "weixin://wxpay/bizpayurl?pr=wx" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.info("微信Native支付二维码: {} (prepay_id={})", simulatedCodeUrl, prepayId);
        return simulatedCodeUrl;
    }

    private String buildWechatH5Pay(PaymentOrder order, int totalCents) {
        String prepayId = createWechatPrepayId(order, totalCents, "MWEB");
        String clientIp = getClientIp();
        String simulatedH5Url = "https://wx.tenpay.com/cgi-bin/mmpayweb-bin/checkmweb?prepay_id=" + prepayId + "&package=&redirect_url=";
        log.info("微信H5支付链接: {} (ip={})", simulatedH5Url, clientIp);
        return simulatedH5Url;
    }

    private Map<String, Object> buildWechatAppPay(PaymentOrder order, int totalCents) {
        String prepayId = createWechatPrepayId(order, totalCents, "APP");
        String nonceStr = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        long timestamp = System.currentTimeMillis() / 1000;

        // 二次签名（App 调起支付需要）
        String signStr = wechatPayConfig.getAppId() + "\n" + timestamp + "\n" + nonceStr + "\n" + prepayId + "\n";
        String paySign = wechatPayConfig.hmacSha256(signStr);

        Map<String, Object> appParams = new LinkedHashMap<>();
        appParams.put("appid", wechatPayConfig.getAppId());
        appParams.put("partnerid", wechatPayConfig.getMchId());
        appParams.put("prepayid", prepayId);
        appParams.put("package", "Sign=WXPay");
        appParams.put("noncestr", nonceStr);
        appParams.put("timestamp", timestamp);
        appParams.put("sign", paySign);
        return appParams;
    }

    private Map<String, Object> buildWechatJsapiPay(PaymentOrder order, int totalCents) {
        String prepayId = createWechatPrepayId(order, totalCents, "JSAPI");
        String nonceStr = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        long timeStamp = System.currentTimeMillis() / 1000;

        // JSAPI 调起支付签名
        String packageStr = "prepay_id=" + prepayId;
        String signStr = wechatPayConfig.getAppId() + "\n" + timeStamp + "\n" + nonceStr + "\n" + packageStr + "\n";
        String paySign = wechatPayConfig.hmacSha256(signStr);

        Map<String, Object> jsapiParams = new LinkedHashMap<>();
        jsapiParams.put("appId", wechatPayConfig.getAppId());
        jsapiParams.put("timeStamp", String.valueOf(timeStamp));
        jsapiParams.put("nonceStr", nonceStr);
        jsapiParams.put("package", packageStr);
        jsapiParams.put("signType", "HMAC-SHA256");
        jsapiParams.put("paySign", paySign);
        return jsapiParams;
    }

    private String createWechatPrepayId(PaymentOrder order, int totalCents, String tradeType) {
        try {
            String url = "/v3/pay/transactions/" + ("NATIVE".equals(tradeType) ? "native" :
                    "MWEB".equals(tradeType) ? "h5" : "jsapi");

            Map<String, Object> reqBody = new LinkedHashMap<>();
            reqBody.put("appid", wechatPayConfig.getAppId());
            reqBody.put("mchid", wechatPayConfig.getMchId());
            reqBody.put("description", order.getSubject());
            reqBody.put("out_trade_no", order.getOrderNo());
            reqBody.put("notify_url", wechatPayConfig.getNotifyUrl());
            reqBody.put("amount", Map.of("total", totalCents, "currency", "CNY"));

            if ("MWEB".equals(tradeType)) {
                Map<String, String> sceneInfo = new LinkedHashMap<>();
                sceneInfo.put("payer_client_ip", getClientIp());
                sceneInfo.put("h5_info", "{\"type\":\"Wap\"}");
                reqBody.put("scene_info", sceneInfo);
            }

            String body = objectMapper.writeValueAsString(reqBody);
            String auth = wechatPayConfig.buildAuthorization("POST", url, body);

            // 模拟 prepay_id，实际部署时调用微信 API
            // String response = RestClient.create(wechatPayConfig.getGatewayUrl())
            //         .post().uri(url).header("Authorization", auth)
            //         .header("Accept", "application/json").header("Content-Type", "application/json")
            //         .body(body).retrieve().body(String.class);
            // return objectMapper.readTree(response).get("prepay_id").asText();

            return "wx_prepay_" + tradeType.toLowerCase() + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        } catch (Exception e) {
            log.error("微信{}支付异常 - 订单号: {}", tradeType, order.getOrderNo(), e);
            throw new RuntimeException("微信支付创建失败: " + e.getMessage());
        }
    }

    @Override
    public String alipayPagePay(String orderNo) {
        Thread currentThread = Thread.currentThread();
        log.info("支付宝页面支付 - 订单号: {}, 线程: {}, 虚拟线程: {}", orderNo, currentThread, currentThread.isVirtual());

        PaymentOrder order = queryOrder(orderNo);
        if (order == null || !"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("订单不存在或状态不正确");
        }

        // 构建支付宝页面支付请求参数
        Map<String, String> bizContent = new LinkedHashMap<>();
        bizContent.put("out_trade_no", orderNo);
        bizContent.put("total_amount", order.getAmount().toString());
        bizContent.put("subject", order.getSubject());
        bizContent.put("body", order.getBody() != null ? order.getBody() : "");
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        bizContent.put("timeout_express", getAlipayTimeout());

        Map<String, String> params = new LinkedHashMap<>();
        params.put("app_id", alipayConfig.getAppId());
        params.put("method", "alipay.trade.page.pay");
        params.put("charset", "UTF-8");
        params.put("sign_type", alipayConfig.getSignType());
        params.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        params.put("version", "1.0");
        params.put("notify_url", alipayConfig.getNotifyUrl());
        params.put("return_url", alipayConfig.getReturnUrl());
        try {
            params.put("biz_content", objectMapper.writeValueAsString(bizContent));
        } catch (Exception e) {
            throw new RuntimeException("构建支付参数失败", e);
        }

        // RSA2签名
        String signContent = alipayConfig.buildSignContent(params);
        String sign = alipayConfig.sign(signContent);
        params.put("sign", sign);

        // 构建自动提交的HTML表单
        StringBuilder html = new StringBuilder();
        html.append("<form id=\"alipayForm\" action=\"").append(alipayConfig.getGatewayUrl())
                .append("\" method=\"POST\">\n");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            html.append("  <input type=\"hidden\" name=\"").append(entry.getKey())
                    .append("\" value=\"").append(escapeHtml(entry.getValue())).append("\"/>\n");
        }
        html.append("  <script>document.getElementById('alipayForm').submit();</script>\n");
        html.append("</form>");

        log.info("支付宝页面支付表单生成成功 - 订单号: {}", orderNo);
        return html.toString();
    }

    @Override
    public String wechatNativePay(String orderNo) {
        Thread currentThread = Thread.currentThread();
        log.info("微信Native支付 - 订单号: {}, 线程: {}, 虚拟线程: {}", orderNo, currentThread, currentThread.isVirtual());

        PaymentOrder order = queryOrder(orderNo);
        if (order == null || !"PENDING".equals(order.getStatus())) {
            throw new RuntimeException("订单不存在或状态不正确");
        }

        try {
            String url = "/v3/pay/transactions/native";
            int totalCents = order.getAmount().multiply(new BigDecimal("100")).intValue();

            Map<String, Object> reqBody = new LinkedHashMap<>();
            reqBody.put("appid", wechatPayConfig.getAppId());
            reqBody.put("mchid", wechatPayConfig.getMchId());
            reqBody.put("description", order.getSubject());
            reqBody.put("out_trade_no", orderNo);
            reqBody.put("notify_url", wechatPayConfig.getNotifyUrl());
            reqBody.put("amount", Map.of("total", totalCents, "currency", "CNY"));

            String body = objectMapper.writeValueAsString(reqBody);
            String auth = wechatPayConfig.buildAuthorization("POST", url, body);

            // 实际调用微信支付API
            // RestClient restClient = RestClient.create(wechatPayConfig.getGatewayUrl());
            // String response = restClient.post()
            //         .uri(url)
            //         .header("Authorization", auth)
            //         .header("Accept", "application/json")
            //         .header("Content-Type", "application/json")
            //         .body(body)
            //         .retrieve()
            //         .body(String.class);
            // return objectMapper.readTree(response).get("code_url").asText();

            // 模拟返回（实际部署时替换为上述HTTP调用）
            String simulatedCodeUrl = "weixin://wxpay/bizpayurl?pr=wx" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            log.info("微信Native支付二维码: {}", simulatedCodeUrl);
            return simulatedCodeUrl;
        } catch (Exception e) {
            log.error("微信Native支付异常 - 订单号: {}", orderNo, e);
            throw new RuntimeException("微信支付创建失败: " + e.getMessage());
        }
    }

    @Override
    public PaymentOrder queryOrder(String orderNo) {
        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentOrder::getOrderNo, orderNo);
        return paymentOrderMapper.selectOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handleAlipayNotify(Map<String, String> params) {
        Thread currentThread = Thread.currentThread();
        log.info("支付宝异步通知 - 线程: {}, 虚拟线程: {}", currentThread, currentThread.isVirtual());

        String orderNo = params.get("out_trade_no");
        if (orderNo == null || orderNo.isBlank()) {
            log.error("支付宝通知缺少订单号");
            saveNotifyLog("ALIPAY", null, params.toString(), null, "FAILED", "通知缺少订单号", getClientIp());
            return "failure";
        }
        String tradeNo = params.get("trade_no");
        String tradeStatus = params.get("trade_status");
        String buyerId = params.get("buyer_id");
        String notifyBody;
        try {
            notifyBody = objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            notifyBody = params.toString();
        }

        PaymentOrder order = queryOrder(orderNo);
        if (order == null) {
            log.error("支付宝通知订单不存在: {}", orderNo);
            saveNotifyLog("ALIPAY", orderNo, notifyBody, null, "ORDER_NOT_FOUND", "订单不存在", getClientIp());
            return "failure";
        }

        // RSA2验签
        String signContent = alipayConfig.buildSignContent(params);
        String sign = params.get("sign");
        boolean sigValid = alipayConfig.verify(signContent, sign);
        if (!sigValid) {
            log.error("支付宝通知验签失败: {}", orderNo);
            saveNotifyLog("ALIPAY", orderNo, notifyBody, false, "SIGN_INVALID", "RSA2 验签失败", getClientIp());
            return "failure";
        }

        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            if ("SUCCESS".equals(order.getStatus())) {
                saveNotifyLog("ALIPAY", orderNo, notifyBody, true, "DUPLICATE", "订单已处理，重复通知", getClientIp());
            } else {
                order.setStatus("SUCCESS");
                order.setTradeNo(tradeNo);
                order.setBuyerId(buyerId);
                order.setPaidTime(LocalDateTime.now());
                order.setNotifyData(notifyBody);
                paymentOrderMapper.updateById(order);
                log.info("支付宝支付成功 - 订单号: {}, 交易号: {}", orderNo, tradeNo);
                saveNotifyLog("ALIPAY", orderNo, notifyBody, true, "PROCESSED", null, getClientIp());
            }
        } else {
            saveNotifyLog("ALIPAY", orderNo, notifyBody, true, "RECEIVED", "交易状态: " + tradeStatus, getClientIp());
        }

        return "success";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> handleWechatNotify(String body, String signature, String serial, String nonce, String timestamp) {
        Thread currentThread = Thread.currentThread();
        log.info("微信支付异步通知 - 线程: {}, 虚拟线程: {}", currentThread, currentThread.isVirtual());

        try {
            // 验签: 使用平台证书公钥验证签名
            // String message = timestamp + "\n" + nonce + "\n" + body + "\n";
            // boolean valid = verifyWechatSign(message, signature, serial);
            // if (!valid) return Map.of("code", "FAIL", "message", "签名验证失败");

            Map<String, Object> notifyData = objectMapper.readValue(body, Map.class);
            String eventType = (String) notifyData.get("event_type");

            if ("TRANSACTION.SUCCESS".equals(eventType)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> transaction = (Map<String, Object>) notifyData.get("resource");
                // 使用APIv3密钥解密resource
                // String decrypted = decryptAes256Gcm(
                //     (String) transaction.get("ciphertext"),
                //     (String) transaction.get("associated_data"),
                //     (String) transaction.get("nonce"),
                //     wechatPayConfig.getApiV3Key());

                // 解密后的数据
                String orderNo = ((Map<String, Object>) transaction.get("out_trade_no")).toString();

                PaymentOrder order = queryOrder(orderNo);
                if (order == null) {
                    log.error("微信通知订单不存在: {}", orderNo);
                    saveNotifyLog("WECHAT", orderNo, body, null, "ORDER_NOT_FOUND", "订单不存在", getClientIp());
                    return Map.of("code", "FAIL", "message", "订单不存在");
                }

                if ("SUCCESS".equals(order.getStatus())) {
                    saveNotifyLog("WECHAT", orderNo, body, null, "DUPLICATE", "订单已处理，重复通知", getClientIp());
                } else {
                    order.setStatus("SUCCESS");
                    order.setTradeNo(((Map<String, Object>) transaction.get("transaction_id")).toString());
                    order.setPaidTime(LocalDateTime.now());
                    order.setNotifyData(body);
                    paymentOrderMapper.updateById(order);
                    log.info("微信支付成功 - 订单号: {}", orderNo);
                    saveNotifyLog("WECHAT", orderNo, body, null, "PROCESSED", null, getClientIp());
                }
            } else {
                saveNotifyLog("WECHAT", null, body, null, "RECEIVED", "事件类型: " + eventType, getClientIp());
            }

            return Map.of("code", "SUCCESS", "message", "OK");
        } catch (Exception e) {
            log.error("微信支付通知处理异常", e);
            saveNotifyLog("WECHAT", null, body, null, "FAILED", e.getClass().getSimpleName() + ": " + e.getMessage(), getClientIp());
            return Map.of("code", "FAIL", "message", e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean closeOrder(String orderNo) {
        PaymentOrder order = queryOrder(orderNo);
        if (order == null || !"PENDING".equals(order.getStatus())) {
            return false;
        }

        String validationError = paymentConfigService.validateConfig(order.getPaymentMethod());
        if (validationError != null) {
            log.warn("关单失败，支付配置不完整 - 订单号: {}, 原因: {}", orderNo, validationError);
            throw new RuntimeException(validationError);
        }

        // 支付宝关单
        if ("ALIPAY".equalsIgnoreCase(order.getPaymentMethod())) {
            Map<String, String> bizContent = new LinkedHashMap<>();
            bizContent.put("out_trade_no", orderNo);

            Map<String, String> params = new LinkedHashMap<>();
            params.put("app_id", alipayConfig.getAppId());
            params.put("method", "alipay.trade.close");
            params.put("charset", "UTF-8");
            params.put("sign_type", alipayConfig.getSignType());
            params.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            params.put("version", "1.0");
            try {
                params.put("biz_content", objectMapper.writeValueAsString(bizContent));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            String signContent = alipayConfig.buildSignContent(params);
            params.put("sign", alipayConfig.sign(signContent));

            // 实际调用: RestClient.create().post().uri(alipayConfig.getGatewayUrl()).body(params)...
            log.info("支付宝关单 - 订单号: {}", orderNo);
        }

        // 微信关单
        if ("WECHAT".equalsIgnoreCase(order.getPaymentMethod())) {
            // 实际调用: POST /v3/pay/transactions/out-trade-no/{orderNo}/close
            log.info("微信关单 - 订单号: {}", orderNo);
        }

        order.setStatus("CLOSED");
        paymentOrderMapper.updateById(order);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> refund(String orderNo, BigDecimal refundAmount, String reason) {
        Thread currentThread = Thread.currentThread();
        log.info("申请退款 - 订单号: {}, 金额: {}, 线程: {}, 虚拟线程: {}",
                orderNo, refundAmount, currentThread, currentThread.isVirtual());

        PaymentOrder order = queryOrder(orderNo);
        if (order == null || !"SUCCESS".equals(order.getStatus())) {
            throw new RuntimeException("订单不存在或未支付");
        }

        String validationError = paymentConfigService.validateConfig(order.getPaymentMethod());
        if (validationError != null) {
            throw new RuntimeException(validationError);
        }

        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("退款金额必须大于0");
        }

        BigDecimal maxRefund = order.getAmount().subtract(
                order.getRefundAmount() != null ? order.getRefundAmount() : BigDecimal.ZERO);
        if (refundAmount.compareTo(maxRefund) > 0) {
            throw new RuntimeException("退款金额超过可退金额");
        }

        String refundTradeNo = generateTradeNo();

        if ("ALIPAY".equalsIgnoreCase(order.getPaymentMethod())) {
            // 支付宝退款: alipay.trade.refund
            Map<String, String> bizContent = new LinkedHashMap<>();
            bizContent.put("out_trade_no", orderNo);
            bizContent.put("trade_no", order.getTradeNo());
            bizContent.put("refund_amount", refundAmount.toString());
            bizContent.put("refund_reason", reason);
            bizContent.put("out_request_no", refundTradeNo);

            Map<String, String> params = new LinkedHashMap<>();
            params.put("app_id", alipayConfig.getAppId());
            params.put("method", "alipay.trade.refund");
            params.put("charset", "UTF-8");
            params.put("sign_type", alipayConfig.getSignType());
            params.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            params.put("version", "1.0");
            try {
                params.put("biz_content", objectMapper.writeValueAsString(bizContent));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            String signContent = alipayConfig.buildSignContent(params);
            params.put("sign", alipayConfig.sign(signContent));

            // 实际调用: POST alipayConfig.getGatewayUrl() with params
            log.info("支付宝退款 - 订单号: {}, 退款单号: {}, 金额: {}", orderNo, refundTradeNo, refundAmount);
        } else {
            // 微信支付退款: POST /v3/refund/domestic/refunds
            log.info("微信退款 - 订单号: {}, 退款单号: {}, 金额: {}", orderNo, refundTradeNo, refundAmount);
        }

        BigDecimal newRefund = (order.getRefundAmount() != null ? order.getRefundAmount() : BigDecimal.ZERO)
                .add(refundAmount);
        order.setRefundAmount(newRefund);
        if (newRefund.compareTo(order.getAmount()) >= 0) {
            order.setStatus("REFUND");
        }
        paymentOrderMapper.updateById(order);

        return Map.of("success", true, "refundTradeNo", refundTradeNo, "amount", refundAmount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int closeExpiredOrders() {
        Thread currentThread = Thread.currentThread();
        log.info("扫描超时未支付订单 - 线程: {}, 虚拟线程: {}", currentThread, currentThread.isVirtual());

        LocalDateTime deadline = getOrderExpireDeadline();

        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentOrder::getStatus, "PENDING")
                .lt(PaymentOrder::getCreateTime, deadline);

        List<PaymentOrder> expiredOrders = paymentOrderMapper.selectList(wrapper);

        if (expiredOrders.isEmpty()) {
            log.debug("无超时未支付订单");
            return 0;
        }

        int expireMinutes = paymentConfigService.getOrderExpireMinutes("ALIPAY");
        log.info("发现 {} 笔超时未支付订单（超过{}分钟），开始批量关闭", expiredOrders.size(), expireMinutes);

        int closedCount = 0;
        for (PaymentOrder order : expiredOrders) {
            try {
                // 调用平台关单接口
                if ("ALIPAY".equalsIgnoreCase(order.getPaymentMethod())) {
                    Map<String, String> bizContent = new LinkedHashMap<>();
                    bizContent.put("out_trade_no", order.getOrderNo());

                    Map<String, String> params = new LinkedHashMap<>();
                    params.put("app_id", alipayConfig.getAppId());
                    params.put("method", "alipay.trade.close");
                    params.put("charset", "UTF-8");
                    params.put("sign_type", alipayConfig.getSignType());
                    params.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    params.put("version", "1.0");
                    params.put("biz_content", objectMapper.writeValueAsString(bizContent));
                    String signContent = alipayConfig.buildSignContent(params);
                    params.put("sign", alipayConfig.sign(signContent));
                    log.debug("支付宝关单 - 订单号: {}", order.getOrderNo());
                } else if ("WECHAT".equalsIgnoreCase(order.getPaymentMethod())) {
                    log.debug("微信关单 - 订单号: {}", order.getOrderNo());
                }

                order.setStatus("CLOSED");
                paymentOrderMapper.updateById(order);
                closedCount++;
                log.info("超时订单已自动关闭 - 订单号: {}, 创建时间: {}, 金额: {}",
                        order.getOrderNo(), order.getCreateTime(), order.getAmount());
            } catch (Exception e) {
                log.error("关闭超时订单失败 - 订单号: {}", order.getOrderNo(), e);
            }
        }

        log.info("批量关闭超时订单完成 - 共 {} 笔，成功 {} 笔", expiredOrders.size(), closedCount);
        return closedCount;
    }

    @Override
    public Page<PaymentOrder> getOrdersByPage(int page, int size) {
        Page<PaymentOrder> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(PaymentOrder::getCreateTime);
        return paymentOrderMapper.selectPage(pageParam, wrapper);
    }

    @Async("taskExecutor")
    @Override
    public CompletableFuture<Map<String, Object>> createPaymentAsync(String subject, String body, BigDecimal amount, String paymentMethod, String tradeType, String bizType, String remark) {
        Thread currentThread = Thread.currentThread();
        log.info("异步创建支付订单 - 方式: {}, 交易类型: {}, 业务: {}, 线程: {}, 虚拟线程: {}",
                paymentMethod, tradeType, bizType, currentThread, currentThread.isVirtual());
        return CompletableFuture.completedFuture(createPayment(subject, body, amount, paymentMethod, tradeType, bizType, remark));
    }

    private LocalDateTime getOrderExpireDeadline() {
        int minutes = paymentConfigService.getOrderExpireMinutes("ALIPAY");
        return LocalDateTime.now().minusMinutes(minutes);
    }

    private String getAlipayTimeout() {
        int minutes = paymentConfigService.getOrderExpireMinutes("ALIPAY");
        return minutes + "m";
    }

    private String generateOrderNo(String paymentMethod) {
        String prefix = "ALIPAY".equalsIgnoreCase(paymentMethod) ? "AL" : "WX";
        return prefix + LocalDateTime.now().format(ORDER_NO_DATE_FMT) +
                String.format("%06d", (int) (Math.random() * 1000000));
    }

    private String generateTradeNo() {
        return "TRD" + LocalDateTime.now().format(ORDER_NO_DATE_FMT) +
                String.format("%06d", (int) (Math.random() * 1000000));
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("\"", "&quot;")
                .replace("<", "&lt;").replace(">", "&gt;");
    }

    // ==================== 回调日志 ====================

    private void saveNotifyLog(String paymentMethod, String orderNo, String notifyBody,
                                Boolean signatureValid, String processStatus, String errorMsg, String ipAddress) {
        try {
            PaymentNotifyLog notifyLog = new PaymentNotifyLog();
            notifyLog.setPaymentMethod(paymentMethod);
            notifyLog.setOrderNo(orderNo);
            notifyLog.setNotifyBody(notifyBody);
            notifyLog.setSignatureValid(signatureValid);
            notifyLog.setProcessStatus(processStatus);
            notifyLog.setErrorMsg(errorMsg);
            notifyLog.setIpAddress(ipAddress);
            notifyLog.setCreateTime(LocalDateTime.now());
            notifyLogMapper.insert(notifyLog);
        } catch (Exception e) {
            log.error("保存回调通知日志失败", e);
        }
    }

    private String getClientIp() {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    @Override
    public Page<PaymentNotifyLog> getNotifyLogsByPage(int page, int size, String paymentMethod, String orderNo) {
        Page<PaymentNotifyLog> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<PaymentNotifyLog> wrapper = new LambdaQueryWrapper<>();
        if (paymentMethod != null && !paymentMethod.isBlank()) {
            wrapper.eq(PaymentNotifyLog::getPaymentMethod, paymentMethod.toUpperCase());
        }
        if (orderNo != null && !orderNo.isBlank()) {
            wrapper.eq(PaymentNotifyLog::getOrderNo, orderNo);
        }
        wrapper.orderByDesc(PaymentNotifyLog::getCreateTime);
        return notifyLogMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public PaymentNotifyLog getNotifyLogById(Long id) {
        return notifyLogMapper.selectById(id);
    }

    @Override
    public int deleteOldNotifyLogs(int beforeDays) {
        LocalDateTime deadline = LocalDateTime.now().minusDays(beforeDays);
        LambdaQueryWrapper<PaymentNotifyLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(PaymentNotifyLog::getCreateTime, deadline);
        return notifyLogMapper.delete(wrapper);
    }
}
