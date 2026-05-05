package org.example.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("reconciliation_record")
public class ReconciliationRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 对帐日期 */
    private LocalDate reconDate;

    /** 支付方式: ALIPAY / WECHAT */
    private String paymentMethod;

    /** 本地总金额 */
    private BigDecimal localTotalAmount;

    /** 平台总金额 */
    private BigDecimal remoteTotalAmount;

    /** 本地订单数 */
    private Integer localCount;

    /** 平台订单数 */
    private Integer remoteCount;

    /** 差额 */
    private BigDecimal diffAmount;

    /** 差异笔数 */
    private Integer diffCount;

    /** 对帐状态: SUCCESS/DIFF/ERROR */
    private String status;

    /** 对帐详情摘要 */
    private String summary;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    @Version
    private Integer version;
}
