package org.example.service;

import org.example.entity.ReconciliationDetail;
import org.example.entity.ReconciliationRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface ReconciliationService {

    /**
     * 执行对帐（指定日期和支付方式）
     * @param reconDate 对帐日期
     * @param paymentMethod 支付方式 ALIPAY/WECHAT
     * @return 对帐记录
     */
    ReconciliationRecord reconcile(LocalDate reconDate, String paymentMethod);

    /**
     * 拉取平台对帐单（模拟）
     * @param reconDate 对帐日期
     * @param paymentMethod 支付方式
     * @return 平台交易列表
     */
    List<Map<String, Object>> fetchRemoteBill(LocalDate reconDate, String paymentMethod);

    /**
     * 比对本地与平台帐单
     * @param reconDate 对帐日期
     * @param paymentMethod 支付方式
     * @param localOrders 本地订单
     * @param remoteOrders 平台订单
     * @return 对帐明细列表
     */
    List<ReconciliationDetail> compareBills(LocalDate reconDate, String paymentMethod,
                                             List<Map<String, Object>> localOrders,
                                             List<Map<String, Object>> remoteOrders);

    /**
     * 查询对帐记录
     */
    ReconciliationRecord getReconciliationRecord(Long id);

    /**
     * 分页查询对帐记录
     */
    List<ReconciliationRecord> getReconciliationRecordsByPage(int page, int size);

    /**
     * 查询对帐明细
     */
    List<ReconciliationDetail> getReconciliationDetails(Long reconRecordId);

    /**
     * 获取对帐统计
     */
    Map<String, Object> getReconciliationStats(LocalDate startDate, LocalDate endDate);

    /**
     * 异步执行对帐 - 虚拟线程
     */
    CompletableFuture<ReconciliationRecord> reconcileAsync(LocalDate reconDate, String paymentMethod);

    /**
     * 获取本地订单数据用于对帐
     */
    List<Map<String, Object>> getLocalOrdersForRecon(LocalDate reconDate, String paymentMethod);
}
