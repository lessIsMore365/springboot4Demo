package org.example.service;

import org.springframework.core.io.Resource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface LogService {

    /**
     * 查看最近的日志行 (tail)
     * @param lines 行数
     * @param level 日志级别过滤，null 表示不过滤
     * @param file 日志文件名，null 默认 application.log
     */
    List<String> tail(int lines, String level, String file);

    /**
     * 搜索日志
     * @param keyword 关键字
     * @param level 级别过滤
     * @param from 开始时间
     * @param to 结束时间
     * @param page 页码
     * @param size 每页条数
     * @param file 日志文件名
     */
    Map<String, Object> search(String keyword, String level, LocalDateTime from, LocalDateTime to,
                                int page, int size, String file);

    /**
     * 列出日志目录下所有日志文件及大小
     */
    List<Map<String, Object>> listFiles();

    /**
     * 下载日志文件
     */
    Resource download(String file);

    /**
     * 获取所有日志记录器及当前级别
     */
    Map<String, String> getLoggers();

    /**
     * 动态修改日志级别
     */
    void setLogLevel(String loggerName, String level);
}
