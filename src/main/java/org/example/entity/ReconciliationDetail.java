package org.example.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("reconciliation_detail")
public class ReconciliationDetail {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联对帐记录ID */
    private Long reconRecordId;

    /** 对帐日期 */
    private LocalDate reconDate;

    /** 商户订单号 */
    private String orderNo;

    /** 支付平台交易号 */
    private String tradeNo;

    /** 本地金额 */
    private BigDecimal localAmount;

    /** 平台金额 */
    private BigDecimal remoteAmount;

    /** 本地状态 */
    private String localStatus;

    /** 平台状态 */
    private String remoteStatus;

    /** 差异类型: MATCH/MISMATCH/LOCAL_ONLY/REMOTE_ONLY */
    private String diffType;

    /** 差异说明 */
    private String diffDesc;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
