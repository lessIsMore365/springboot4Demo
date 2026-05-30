package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.entity.AiApiUsage;

import java.math.BigDecimal;
import java.util.Map;

@Mapper
public interface AiApiUsageMapper extends BaseMapper<AiApiUsage> {

    @Select("SELECT COALESCE(SUM(total_tokens),0) as total_tokens, COALESCE(SUM(cost),0) as total_cost, COUNT(*) as call_count FROM ai_api_usage")
    Map<String, Object> selectSummary();

    @Select("SELECT model, COUNT(*) as call_count, COALESCE(SUM(total_tokens),0) as total_tokens, COALESCE(SUM(cost),0) as total_cost FROM ai_api_usage GROUP BY model ORDER BY call_count DESC")
    java.util.List<Map<String, Object>> selectSummaryByModel();
}
