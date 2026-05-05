package org.example.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.ReconciliationRecord;
import org.example.service.ReconciliationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 每日自动对帐调度器
 * 每天凌晨2:00自动执行前一天的支付宝和微信支付对帐
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReconciliationScheduler {

    private final ReconciliationService reconciliationService;

    /**
     * 每日自动对帐 - 支付宝
     * 每天凌晨 2:00 执行，对帐前一天的数据
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void dailyAlipayReconciliation() {
        Thread currentThread = Thread.currentThread();
        log.info("===== 支付宝每日自动对帐开始 ===== 线程: {}, 虚拟线程: {}",
                currentThread, currentThread.isVirtual());

        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            ReconciliationRecord record = reconciliationService.reconcile(yesterday, "ALIPAY");

            if ("SUCCESS".equals(record.getStatus())) {
                log.info("支付宝对帐完成 - 一致, 共{}笔, 金额¥{}",
                        record.getLocalCount(), record.getLocalTotalAmount());
            } else {
                log.warn("支付宝对帐完成 - 存在差异! 差异{}笔, 差额¥{}",
                        record.getDiffCount(), record.getDiffAmount());
            }
        } catch (Exception e) {
            log.error("支付宝每日自动对帐失败", e);
        }

        log.info("===== 支付宝每日自动对帐结束 =====");
    }

    /**
     * 每日自动对帐 - 微信支付
     * 每天凌晨 3:00 执行，对帐前一天的数据
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void dailyWechatReconciliation() {
        Thread currentThread = Thread.currentThread();
        log.info("===== 微信支付每日自动对帐开始 ===== 线程: {}, 虚拟线程: {}",
                currentThread, currentThread.isVirtual());

        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            ReconciliationRecord record = reconciliationService.reconcile(yesterday, "WECHAT");

            if ("SUCCESS".equals(record.getStatus())) {
                log.info("微信支付对帐完成 - 一致, 共{}笔, 金额¥{}",
                        record.getLocalCount(), record.getLocalTotalAmount());
            } else {
                log.warn("微信支付对帐完成 - 存在差异! 差异{}笔, 差额¥{}",
                        record.getDiffCount(), record.getDiffAmount());
            }
        } catch (Exception e) {
            log.error("微信支付每日自动对帐失败", e);
        }

        log.info("===== 微信支付每日自动对帐结束 =====");
    }

    /**
     * 健康检查定时任务
     * 每30分钟打印一次服务状态
     */
    @Scheduled(cron = "0 */30 * * * ?")
    public void healthMonitor() {
        log.debug("对帐调度器运行正常 - {}", LocalDate.now());
    }
}
