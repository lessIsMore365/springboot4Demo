package org.example.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.entity.AiApiUsage;
import org.example.mapper.AiApiUsageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AiUsageService {

    private static final Logger log = LoggerFactory.getLogger(AiUsageService.class);
    private final AiApiUsageMapper mapper;

    public AiUsageService(AiApiUsageMapper mapper) {
        this.mapper = mapper;
    }

    public void record(String model, String endpoint, int promptTokens, int completionTokens,
                       int latencyMs, Long userId, String username, boolean success, String errorMsg,
                       double costPerMillionTokens) {
        try {
            AiApiUsage usage = new AiApiUsage();
            usage.setModel(model);
            usage.setEndpoint(endpoint);
            usage.setPromptTokens(promptTokens);
            usage.setCompletionTokens(completionTokens);
            usage.setTotalTokens(promptTokens + completionTokens);
            BigDecimal cost = BigDecimal.valueOf((promptTokens + completionTokens) * costPerMillionTokens / 1_000_000.0)
                    .setScale(6, RoundingMode.HALF_UP);
            usage.setCost(cost);
            usage.setLatencyMs(latencyMs);
            usage.setUserId(userId);
            usage.setUsername(username);
            usage.setSuccess(success);
            usage.setErrorMsg(errorMsg);
            mapper.insert(usage);
        } catch (Exception e) {
            log.warn("Failed to record AI API usage: {}", e.getMessage());
        }
    }

    public IPage<AiApiUsage> page(int page, int size, String model, String username) {
        QueryWrapper<AiApiUsage> qw = new QueryWrapper<>();
        if (model != null && !model.isBlank()) qw.eq("model", model);
        if (username != null && !username.isBlank()) qw.eq("username", username);
        qw.orderByDesc("create_time");
        return mapper.selectPage(new Page<>(page, size), qw);
    }

    public Map<String, Object> getSummary() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", mapper.selectSummary());
        result.put("byModel", mapper.selectSummaryByModel());
        return result;
    }

    public int clean(int beforeDays) {
        String cutoff = LocalDate.now().minusDays(beforeDays).format(DateTimeFormatter.ISO_LOCAL_DATE);
        QueryWrapper<AiApiUsage> qw = new QueryWrapper<>();
        qw.lt("create_time", cutoff);
        return mapper.delete(qw);
    }
}
