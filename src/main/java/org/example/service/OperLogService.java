package org.example.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.entity.SysOperLog;

public interface OperLogService {

    Page<SysOperLog> getLogsByPage(int page, int size, String operName, String title,
                                   String businessType, Integer status);

    SysOperLog getLogById(Long id);

    int deleteOldLogs(int beforeDays);
}
