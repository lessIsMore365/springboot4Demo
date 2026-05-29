package org.example.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.entity.PaymentNotifyLog;
import org.example.entity.PaymentOrder;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface PaymentService {

    /**
     * 创建支付订单
     * @param subject 商品标题
     * @param body 商品描述
     * @param amount 金额
     * @param paymentMethod 支付方式 ALIPAY/WECHAT
     * @return 支付订单（含支付链接/二维码等信息）
     */
    Map<String, Object> createPayment(String subject, String body, BigDecimal amount, String paymentMethod);

    /**
     * 支付宝页面支付
     * @param orderNo 订单号
     * @return 支付页面HTML表单
     */
    String alipayPagePay(String orderNo);

    /**
     * 微信Native扫码支付
     * @param orderNo 订单号
     * @return 二维码链接
     */
    String wechatNativePay(String orderNo);

    /**
     * 查询订单状态
     * @param orderNo 订单号
     * @return 订单信息
     */
    PaymentOrder queryOrder(String orderNo);

    /**
     * 支付宝异步通知处理
     * @param params 通知参数
     * @return 处理结果
     */
    String handleAlipayNotify(Map<String, String> params);

    /**
     * 微信支付异步通知处理
     * @param body 通知报文
     * @param signature 签名
     * @param serial 证书序列号
     * @param nonce 随机串
     * @param timestamp 时间戳
     * @return 处理结果
     */
    Map<String, Object> handleWechatNotify(String body, String signature, String serial, String nonce, String timestamp);

    /**
     * 关闭订单
     * @param orderNo 订单号
     * @return 是否成功
     */
    boolean closeOrder(String orderNo);

    /**
     * 申请退款
     * @param orderNo 订单号
     * @param refundAmount 退款金额
     * @param reason 退款原因
     * @return 退款结果
     */
    Map<String, Object> refund(String orderNo, BigDecimal refundAmount, String reason);

    /**
     * 分页查询支付订单
     */
    Page<PaymentOrder> getOrdersByPage(int page, int size);

    /**
     * 异步创建支付订单 - 虚拟线程
     */
    CompletableFuture<Map<String, Object>> createPaymentAsync(String subject, String body, BigDecimal amount, String paymentMethod);

    /**
     * 自动关闭超时未支付订单（超过45分钟）
     * @return 关闭的订单数量
     */
    int closeExpiredOrders();

    /**
     * 分页查询回调通知日志
     */
    Page<PaymentNotifyLog> getNotifyLogsByPage(int page, int size, String paymentMethod, String orderNo);

    /**
     * 查询单条回调日志
     */
    PaymentNotifyLog getNotifyLogById(Long id);

    /**
     * 删除指定天数之前的回调日志
     */
    int deleteOldNotifyLogs(int beforeDays);
}
