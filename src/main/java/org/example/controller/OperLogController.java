package org.example.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.entity.SysOperLog;
import org.example.service.OperLogService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/monitor/operlog")
@RequiredArgsConstructor
public class OperLogController {

    private final OperLogService operLogService;

    /** 分页查询操作日志 */
    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestParam(required = false) String operName,
                                    @RequestParam(required = false) String title,
                                    @RequestParam(required = false) String businessType,
                                    @RequestParam(required = false) Integer status) {
        Page<SysOperLog> logPage = operLogService.getLogsByPage(page, size, operName, title, businessType, status);
        return Map.of(
                "success", true,
                "data", logPage.getRecords(),
                "pagination", Map.of("page", logPage.getCurrent(), "size", logPage.getSize(),
                        "total", logPage.getTotal(), "pages", logPage.getPages()),
                "timestamp", System.currentTimeMillis()
        );
    }

    /** 查询操作日志详情 */
    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        SysOperLog logEntry = operLogService.getLogById(id);
        return logEntry == null
                ? Map.of("success", false, "message", "日志不存在", "timestamp", System.currentTimeMillis())
                : Map.of("success", true, "data", logEntry, "timestamp", System.currentTimeMillis());
    }

    /** 清理旧操作日志 */
    @DeleteMapping
    public Map<String, Object> clean(@RequestParam(defaultValue = "90") int beforeDays) {
        int deleted = operLogService.deleteOldLogs(beforeDays);
        return Map.of("success", true, "message", "已清理 " + beforeDays + " 天前的操作日志，共 " + deleted + " 条",
                "deletedCount", deleted, "timestamp", System.currentTimeMillis());
    }
}
