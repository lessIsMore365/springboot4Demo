package org.example.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.PaymentEventLog;
import org.example.mapper.PaymentEventLogMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 支付事件记录器 — 异步记录每次状态变更
 * 用于审计追踪：谁、什么时候、做了什么操作、从什么状态变成什么状态
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventRecorder {

    private final PaymentEventLogMapper eventLogMapper;
    private final HttpServletRequest request;
    private final ObjectMapper objectMapper;

    @Async("taskExecutor")
    public void record(String orderNo, String eventType, String fromStatus, String toStatus,
                       String operator, Object eventData) {
        try {
            PaymentEventLog eventLog = new PaymentEventLog();
            eventLog.setOrderNo(orderNo);
            eventLog.setEventType(eventType);
            eventLog.setFromStatus(fromStatus);
            eventLog.setToStatus(toStatus);
            eventLog.setOperator(operator != null ? operator : getCurrentUser());
            eventLog.setOperatorIp(getClientIp());
            if (eventData != null) {
                eventLog.setEventData(objectMapper.writeValueAsString(eventData));
            }
            eventLog.setCreateTime(LocalDateTime.now());
            eventLogMapper.insert(eventLog);
        } catch (Exception e) {
            log.error("记录支付事件失败 — orderNo={}, eventType={}", orderNo, eventType, e);
        }
    }

    /** 便捷方法：记录状态转换事件 */
    public void recordTransition(String orderNo, String fromStatus, String toStatus, Object data) {
        record(orderNo, "STATUS_TRANSITION", fromStatus, toStatus, null, data);
    }

    private String getCurrentUser() {
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            if (auth != null) return auth.getName();
        } catch (Exception ignored) {}
        return "SYSTEM";
    }

    private String getClientIp() {
        if (request == null) return "unknown";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
