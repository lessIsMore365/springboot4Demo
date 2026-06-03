package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.entity.PaymentOrder;

import java.util.List;
import java.util.Map;

@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {

    @Select("""
        <script>
        SELECT COUNT(*) as totalOrders,
               COALESCE(SUM(amount),0) as totalAmount,
               COUNT(CASE WHEN status='SUCCESS' THEN 1 END) as successCount,
               COALESCE(SUM(CASE WHEN status='SUCCESS' THEN amount ELSE 0 END),0) as successAmount,
               COUNT(CASE WHEN status='REFUND' THEN 1 END) as refundCount,
               COALESCE(SUM(CASE WHEN status='REFUND' THEN amount ELSE 0 END),0) as refundAmount,
               COUNT(CASE WHEN status='PENDING' THEN 1 END) as pendingCount,
               COUNT(CASE WHEN status='CLOSED' THEN 1 END) as closedCount
        FROM payment_order WHERE deleted=0
        AND create_time BETWEEN #{start} AND #{end}
        <if test='method != null and method != ""'>
            AND payment_method = #{method}
        </if>
        </script>
        """)
    Map<String, Object> selectOverview(@Param("start") java.time.LocalDateTime start,
                                       @Param("end") java.time.LocalDateTime end,
                                       @Param("method") String method);

    @Select("""
        <script>
        SELECT TO_CHAR(create_time, 'YYYY-MM-DD') as date,
               COUNT(*) as orderCount,
               COALESCE(SUM(amount),0) as totalAmount,
               COALESCE(SUM(CASE WHEN status='SUCCESS' THEN amount ELSE 0 END),0) as successAmount,
               COUNT(CASE WHEN status='SUCCESS' THEN 1 END) as successCount
        FROM payment_order WHERE deleted=0
        AND create_time BETWEEN #{start} AND #{end}
        <if test='method != null and method != ""'>
            AND payment_method = #{method}
        </if>
        GROUP BY TO_CHAR(create_time, 'YYYY-MM-DD')
        ORDER BY date
        </script>
        """)
    List<Map<String, Object>> selectDailyTrend(@Param("start") java.time.LocalDateTime start,
                                               @Param("end") java.time.LocalDateTime end,
                                               @Param("method") String method);

    @Select("""
        <script>
        SELECT payment_method as method,
               COUNT(*) as orderCount,
               COALESCE(SUM(amount),0) as totalAmount,
               COALESCE(SUM(CASE WHEN status='SUCCESS' THEN amount ELSE 0 END),0) as successAmount,
               COUNT(CASE WHEN status='SUCCESS' THEN 1 END) as successCount
        FROM payment_order WHERE deleted=0
        AND create_time BETWEEN #{start} AND #{end}
        GROUP BY payment_method
        </script>
        """)
    List<Map<String, Object>> selectStatsByMethod(@Param("start") java.time.LocalDateTime start,
                                                  @Param("end") java.time.LocalDateTime end);

    @Select("""
        <script>
        SELECT biz_type as bizType,
               COUNT(*) as orderCount,
               COALESCE(SUM(amount),0) as totalAmount,
               COALESCE(SUM(CASE WHEN status='SUCCESS' THEN amount ELSE 0 END),0) as successAmount
        FROM payment_order WHERE deleted=0
        AND biz_type IS NOT NULL
        AND create_time BETWEEN #{start} AND #{end}
        <if test='method != null and method != ""'>
            AND payment_method = #{method}
        </if>
        GROUP BY biz_type
        ORDER BY totalAmount DESC
        </script>
        """)
    List<Map<String, Object>> selectStatsByBizType(@Param("start") java.time.LocalDateTime start,
                                                   @Param("end") java.time.LocalDateTime end,
                                                   @Param("method") String method);

    @Select("""
        <script>
        SELECT status,
               COUNT(*) as orderCount,
               COALESCE(SUM(amount),0) as totalAmount
        FROM payment_order WHERE deleted=0
        AND create_time BETWEEN #{start} AND #{end}
        <if test='method != null and method != ""'>
            AND payment_method = #{method}
        </if>
        GROUP BY status
        ORDER BY
            CASE status
                WHEN 'SUCCESS' THEN 1
                WHEN 'PENDING' THEN 2
                WHEN 'REFUND' THEN 3
                WHEN 'CLOSED' THEN 4
            END
        </script>
        """)
    List<Map<String, Object>> selectStatsByStatus(@Param("start") java.time.LocalDateTime start,
                                                  @Param("end") java.time.LocalDateTime end,
                                                  @Param("method") String method);

    @Select("""
        SELECT order_no, subject, amount, payment_method, status, biz_type, create_time
        FROM payment_order WHERE deleted=0
        ORDER BY create_time DESC LIMIT 10
        """)
    List<Map<String, Object>> selectRecentOrders();
}
