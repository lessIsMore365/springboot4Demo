package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.PaymentOrder;
import org.example.entity.ReconciliationDetail;
import org.example.entity.ReconciliationRecord;
import org.example.mapper.PaymentOrderMapper;
import org.example.mapper.ReconciliationDetailMapper;
import org.example.mapper.ReconciliationRecordMapper;
import org.example.service.ReconciliationService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationServiceImpl implements ReconciliationService {

    private final PaymentOrderMapper paymentOrderMapper;
    private final ReconciliationRecordMapper reconRecordMapper;
    private final ReconciliationDetailMapper reconDetailMapper;

    private static final Set<String> VALID_METHODS = Set.of("ALIPAY", "WECHAT");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationRecord reconcile(LocalDate reconDate, String paymentMethod) {
        Thread currentThread = Thread.currentThread();
        log.info("开始对帐 - 日期: {}, 方式: {}, 线程: {}, 虚拟线程: {}",
                reconDate, paymentMethod, currentThread, currentThread.isVirtual());

        if (!VALID_METHODS.contains(paymentMethod.toUpperCase())) {
            throw new RuntimeException("不支持的支付方式: " + paymentMethod + "，仅支持 ALIPAY / WECHAT");
        }

        // 1. 获取本地订单
        List<Map<String, Object>> localOrders = getLocalOrdersForRecon(reconDate, paymentMethod);

        // 2. 获取平台帐单
        List<Map<String, Object>> remoteOrders = fetchRemoteBill(reconDate, paymentMethod);

        // 3. 逐笔比对
        List<ReconciliationDetail> details = compareBills(reconDate, paymentMethod, localOrders, remoteOrders);

        // 4. 汇总统计
        BigDecimal localTotal = localOrders.stream()
                .map(o -> (BigDecimal) o.get("amount"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remoteTotal = remoteOrders.stream()
                .map(o -> (BigDecimal) o.get("amount"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long matchCount = details.stream().filter(d -> "MATCH".equals(d.getDiffType())).count();
        long mismatchCount = details.stream().filter(d -> "MISMATCH".equals(d.getDiffType())).count();
        long localOnlyCount = details.stream().filter(d -> "LOCAL_ONLY".equals(d.getDiffType())).count();
        long remoteOnlyCount = details.stream().filter(d -> "REMOTE_ONLY".equals(d.getDiffType())).count();
        long diffCount = mismatchCount + localOnlyCount + remoteOnlyCount;

        BigDecimal diffAmount = localTotal.subtract(remoteTotal).abs();

        // 5. 保存对帐记录
        ReconciliationRecord record = new ReconciliationRecord();
        record.setReconDate(reconDate);
        record.setPaymentMethod(paymentMethod.toUpperCase());
        record.setLocalTotalAmount(localTotal.setScale(2, RoundingMode.HALF_UP));
        record.setRemoteTotalAmount(remoteTotal.setScale(2, RoundingMode.HALF_UP));
        record.setLocalCount(localOrders.size());
        record.setRemoteCount(remoteOrders.size());
        record.setDiffAmount(diffAmount.setScale(2, RoundingMode.HALF_UP));
        record.setDiffCount((int) diffCount);
        record.setStatus(diffCount == 0 ? "SUCCESS" : "DIFF");
        record.setSummary(String.format(
                "对帐完成: 本地%d笔/¥%s, 平台%d笔/¥%s, 一致%d笔, 金额不符%d笔, 本地独有%d笔, 平台独有%d笔, 差额¥%s",
                localOrders.size(), localTotal, remoteOrders.size(), remoteTotal,
                matchCount, mismatchCount, localOnlyCount, remoteOnlyCount, diffAmount
        ));
        reconRecordMapper.insert(record);

        // 6. 保存对帐明细
        for (ReconciliationDetail detail : details) {
            detail.setReconRecordId(record.getId());
            reconDetailMapper.insert(detail);
        }

        log.info("对帐完成 - 日期: {}, 方式: {}, 状态: {}, 差异: {}笔",
                reconDate, paymentMethod, record.getStatus(), diffCount);

        return record;
    }

    @Override
    public List<Map<String, Object>> fetchRemoteBill(LocalDate reconDate, String paymentMethod) {
        Thread currentThread = Thread.currentThread();
        log.info("拉取{}平台帐单 - 日期: {}, 线程: {}, 虚拟线程: {}",
                paymentMethod, reconDate, currentThread, currentThread.isVirtual());

        // 实际接入: 调用支付宝/微信支付的对帐单下载接口
        // 支付宝: alipayClient.execute(new AlipayDataDataserviceBillDownloadurlQueryRequest())
        // 微信: GET https://api.mch.weixin.qq.com/v3/bill/tradebill?bill_date={date}

        // 模拟平台返回的帐单数据（基于本地订单 + 随机差异模拟）
        List<Map<String, Object>> remoteOrders = new ArrayList<>();

        // 获取本地的成功支付订单作为基准
        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentOrder::getPaymentMethod, paymentMethod.toUpperCase())
                .eq(PaymentOrder::getStatus, "SUCCESS")
                .ge(PaymentOrder::getCreateTime, reconDate.atStartOfDay())
                .lt(PaymentOrder::getCreateTime, reconDate.plusDays(1).atStartOfDay());
        List<PaymentOrder> localPaid = paymentOrderMapper.selectList(wrapper);

        for (PaymentOrder order : localPaid) {
            Map<String, Object> remote = new HashMap<>();
            remote.put("orderNo", order.getOrderNo());
            remote.put("tradeNo", order.getTradeNo() != null ? order.getTradeNo() : "SIM_" + UUID.randomUUID().toString().substring(0, 8));
            // 模拟: 95%概率金额一致, 5%概率金额偏差
            if (Math.random() < 0.95) {
                remote.put("amount", order.getAmount());
                remote.put("status", "SUCCESS");
            } else {
                remote.put("amount", order.getAmount().add(new BigDecimal("0.01")));
                remote.put("status", "SUCCESS");
            }
            remoteOrders.add(remote);
        }

        // 模拟平台独有订单（10%概率）
        if (Math.random() < 0.3) {
            Map<String, Object> extraOrder = new HashMap<>();
            extraOrder.put("orderNo", "REMOTE_" + UUID.randomUUID().toString().substring(0, 12));
            extraOrder.put("tradeNo", "TRD_REMOTE_" + UUID.randomUUID().toString().substring(0, 8));
            extraOrder.put("amount", new BigDecimal(String.format("%.2f", 10 + Math.random() * 100)));
            extraOrder.put("status", "SUCCESS");
            remoteOrders.add(extraOrder);
        }

        log.info("平台帐单获取完成 - {}平台: {}笔", paymentMethod, remoteOrders.size());
        return remoteOrders;
    }

    @Override
    public List<ReconciliationDetail> compareBills(LocalDate reconDate, String paymentMethod,
                                                     List<Map<String, Object>> localOrders,
                                                     List<Map<String, Object>> remoteOrders) {
        List<ReconciliationDetail> details = new ArrayList<>();

        // 构建远程订单Map (orderNo -> order)
        Map<String, Map<String, Object>> remoteMap = remoteOrders.stream()
                .collect(Collectors.toMap(
                        o -> o.get("orderNo").toString(),
                        o -> o,
                        (a, b) -> a
                ));

        // 构建本地订单Map
        Map<String, Map<String, Object>> localMap = localOrders.stream()
                .collect(Collectors.toMap(
                        o -> o.get("orderNo").toString(),
                        o -> o,
                        (a, b) -> a
                ));

        Set<String> allOrderNos = new HashSet<>();
        allOrderNos.addAll(localMap.keySet());
        allOrderNos.addAll(remoteMap.keySet());

        for (String orderNo : allOrderNos) {
            Map<String, Object> local = localMap.get(orderNo);
            Map<String, Object> remote = remoteMap.get(orderNo);

            ReconciliationDetail detail = new ReconciliationDetail();
            detail.setReconDate(reconDate);
            detail.setOrderNo(orderNo);

            if (local != null && remote != null) {
                // 双方都有
                detail.setLocalAmount((BigDecimal) local.get("amount"));
                detail.setRemoteAmount((BigDecimal) remote.get("amount"));
                detail.setLocalStatus(local.get("status").toString());
                detail.setRemoteStatus(remote.get("status").toString());
                detail.setTradeNo(remote.get("tradeNo") != null ? remote.get("tradeNo").toString() : null);

                if (detail.getLocalAmount().compareTo(detail.getRemoteAmount()) == 0
                        && detail.getLocalStatus().equals(detail.getRemoteStatus())) {
                    detail.setDiffType("MATCH");
                    detail.setDiffDesc("一致");
                } else {
                    detail.setDiffType("MISMATCH");
                    detail.setDiffDesc(String.format("金额不符: 本地%s/平台%s, 状态: 本地%s/平台%s",
                            local.get("amount"), remote.get("amount"),
                            local.get("status"), remote.get("status")));
                }
            } else if (local != null) {
                // 仅本地有
                detail.setLocalAmount((BigDecimal) local.get("amount"));
                detail.setRemoteAmount(BigDecimal.ZERO);
                detail.setLocalStatus(local.get("status").toString());
                detail.setRemoteStatus("-");
                detail.setDiffType("LOCAL_ONLY");
                detail.setDiffDesc("平台无此订单");
            } else {
                // 仅平台有
                detail.setLocalAmount(BigDecimal.ZERO);
                detail.setRemoteAmount((BigDecimal) remote.get("amount"));
                detail.setLocalStatus("-");
                detail.setRemoteStatus(remote.get("status").toString());
                detail.setTradeNo(remote.get("tradeNo") != null ? remote.get("tradeNo").toString() : null);
                detail.setDiffType("REMOTE_ONLY");
                detail.setDiffDesc("本地无此订单");
            }

            details.add(detail);
        }

        return details;
    }

    @Override
    public ReconciliationRecord getReconciliationRecord(Long id) {
        return reconRecordMapper.selectById(id);
    }

    @Override
    public List<ReconciliationRecord> getReconciliationRecordsByPage(int page, int size) {
        Page<ReconciliationRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ReconciliationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ReconciliationRecord::getReconDate)
                .orderByDesc(ReconciliationRecord::getCreateTime);
        Page<ReconciliationRecord> result = reconRecordMapper.selectPage(pageParam, wrapper);
        return result.getRecords();
    }

    @Override
    public List<ReconciliationDetail> getReconciliationDetails(Long reconRecordId) {
        LambdaQueryWrapper<ReconciliationDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReconciliationDetail::getReconRecordId, reconRecordId)
                .orderByAsc(ReconciliationDetail::getDiffType)
                .orderByAsc(ReconciliationDetail::getOrderNo);
        return reconDetailMapper.selectList(wrapper);
    }

    @Override
    public Map<String, Object> getReconciliationStats(LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<ReconciliationRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(ReconciliationRecord::getReconDate, startDate)
                .le(ReconciliationRecord::getReconDate, endDate);

        List<ReconciliationRecord> records = reconRecordMapper.selectList(wrapper);

        long successCount = records.stream().filter(r -> "SUCCESS".equals(r.getStatus())).count();
        long diffCount = records.stream().filter(r -> "DIFF".equals(r.getStatus())).count();
        long errorCount = records.stream().filter(r -> "ERROR".equals(r.getStatus())).count();
        BigDecimal totalDiff = records.stream()
                .map(ReconciliationRecord::getDiffAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Map.of(
                "totalRecords", records.size(),
                "successCount", successCount,
                "diffCount", diffCount,
                "errorCount", errorCount,
                "totalDiffAmount", totalDiff.setScale(2, RoundingMode.HALF_UP),
                "startDate", startDate.toString(),
                "endDate", endDate.toString()
        );
    }

    @Async("taskExecutor")
    @Override
    public CompletableFuture<ReconciliationRecord> reconcileAsync(LocalDate reconDate, String paymentMethod) {
        Thread currentThread = Thread.currentThread();
        log.info("异步对帐 - 日期: {}, 方式: {}, 线程: {}, 虚拟线程: {}",
                reconDate, paymentMethod, currentThread, currentThread.isVirtual());
        return CompletableFuture.completedFuture(reconcile(reconDate, paymentMethod));
    }

    @Override
    public List<Map<String, Object>> getLocalOrdersForRecon(LocalDate reconDate, String paymentMethod) {
        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentOrder::getPaymentMethod, paymentMethod.toUpperCase())
                .eq(PaymentOrder::getStatus, "SUCCESS")
                .ge(PaymentOrder::getPaidTime, reconDate.atStartOfDay())
                .lt(PaymentOrder::getPaidTime, reconDate.plusDays(1).atStartOfDay());
        List<PaymentOrder> orders = paymentOrderMapper.selectList(wrapper);

        return orders.stream().map(o -> {
            Map<String, Object> map = new HashMap<>();
            map.put("orderNo", o.getOrderNo());
            map.put("tradeNo", o.getTradeNo());
            map.put("amount", o.getAmount());
            map.put("status", o.getStatus());
            return map;
        }).collect(Collectors.toList());
    }
}
