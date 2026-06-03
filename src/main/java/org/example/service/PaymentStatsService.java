package org.example.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface PaymentStatsService {

    Map<String, Object> getOverview(LocalDateTime start, LocalDateTime end, String method);

    List<Map<String, Object>> getTrend(LocalDateTime start, LocalDateTime end, String method);

    List<Map<String, Object>> getStatsByMethod(LocalDateTime start, LocalDateTime end);

    List<Map<String, Object>> getStatsByBizType(LocalDateTime start, LocalDateTime end, String method);

    List<Map<String, Object>> getStatsByStatus(LocalDateTime start, LocalDateTime end, String method);

    List<Map<String, Object>> getRecentOrders();
}
