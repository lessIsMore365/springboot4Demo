package org.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.entity.PaymentWebhook;

@Mapper
public interface PaymentWebhookMapper extends BaseMapper<PaymentWebhook> {
}
