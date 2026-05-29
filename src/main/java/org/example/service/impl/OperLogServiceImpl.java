package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.entity.SysOperLog;
import org.example.mapper.SysOperLogMapper;
import org.example.service.OperLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OperLogServiceImpl implements OperLogService {

    private final SysOperLogMapper operLogMapper;

    @Override
    public Page<SysOperLog> getLogsByPage(int page, int size, String operName, String title,
                                          String businessType, Integer status) {
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        if (operName != null && !operName.isBlank())
            wrapper.eq(SysOperLog::getOperName, operName);
        if (title != null && !title.isBlank())
            wrapper.like(SysOperLog::getTitle, title);
        if (businessType != null && !businessType.isBlank())
            wrapper.eq(SysOperLog::getBusinessType, businessType);
        if (status != null)
            wrapper.eq(SysOperLog::getStatus, status);
        wrapper.orderByDesc(SysOperLog::getCreateTime);
        return operLogMapper.selectPage(Page.of(page, size), wrapper);
    }

    @Override
    public SysOperLog getLogById(Long id) {
        return operLogMapper.selectById(id);
    }

    @Override
    public int deleteOldLogs(int beforeDays) {
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(SysOperLog::getCreateTime, LocalDateTime.now().minusDays(beforeDays));
        return operLogMapper.delete(wrapper);
    }
}
