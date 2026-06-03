package org.example.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.mapper.PaymentOrderMapper;
import org.example.service.PaymentStatsService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentStatsServiceImpl implements PaymentStatsService {

    private final PaymentOrderMapper paymentOrderMapper;

    @Override
    public Map<String, Object> getOverview(LocalDateTime start, LocalDateTime end, String method) {
        return paymentOrderMapper.selectOverview(start, end, method);
    }

    @Override
    public List<Map<String, Object>> getTrend(LocalDateTime start, LocalDateTime end, String method) {
        return paymentOrderMapper.selectDailyTrend(start, end, method);
    }

    @Override
    public List<Map<String, Object>> getStatsByMethod(LocalDateTime start, LocalDateTime end) {
        return paymentOrderMapper.selectStatsByMethod(start, end);
    }

    @Override
    public List<Map<String, Object>> getStatsByBizType(LocalDateTime start, LocalDateTime end, String method) {
        return paymentOrderMapper.selectStatsByBizType(start, end, method);
    }

    @Override
    public List<Map<String, Object>> getStatsByStatus(LocalDateTime start, LocalDateTime end, String method) {
        return paymentOrderMapper.selectStatsByStatus(start, end, method);
    }

    @Override
    public List<Map<String, Object>> getRecentOrders() {
        return paymentOrderMapper.selectRecentOrders();
    }
}
