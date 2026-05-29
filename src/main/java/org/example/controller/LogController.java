package org.example.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.service.LogService;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    /**
     * 查看最近日志 (tail)
     */
    @GetMapping("/tail")
    public Map<String, Object> tail(@RequestParam(defaultValue = "100") int lines,
                                     @RequestParam(required = false) String level,
                                     @RequestParam(required = false) String file) {
        List<String> logLines = logService.tail(lines, level, file);
        return Map.of(
                "success", true,
                "data", Map.of(
                        "lines", logLines,
                        "count", logLines.size(),
                        "file", file != null ? file : "application.log"
                ),
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 搜索日志
     */
    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) String level,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       @RequestParam(required = false) String file) {
        Map<String, Object> result = logService.search(keyword, level, from, to, page, size, file);
        result.put("success", true);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    /**
     * 列出所有日志文件
     */
    @GetMapping("/files")
    public Map<String, Object> listFiles() {
        List<Map<String, Object>> files = logService.listFiles();
        return Map.of(
                "success", true,
                "data", files,
                "total", files.size(),
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 下载日志文件
     */
    @GetMapping("/download")
    public void download(@RequestParam(required = false) String file,
                         HttpServletResponse response) throws IOException {
        Resource resource = logService.download(file);
        String filename = file != null ? file : "application.log";

        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + filename + "\"");
        response.setContentLengthLong(resource.contentLength());

        try (OutputStream out = response.getOutputStream()) {
            resource.getInputStream().transferTo(out);
            out.flush();
        }
    }

    /**
     * 获取所有日志记录器及级别
     */
    @GetMapping("/loggers")
    public Map<String, Object> getLoggers() {
        return Map.of(
                "success", true,
                "data", logService.getLoggers(),
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 动态修改日志级别
     */
    @PutMapping("/loggers/{loggerName}")
    public Map<String, Object> setLogLevel(@PathVariable String loggerName,
                                            @RequestBody Map<String, String> body) {
        String level = body.get("level");
        logService.setLogLevel(loggerName, level);
        return Map.of(
                "success", true,
                "message", "日志级别已修改 — " + loggerName + " → " + level.toUpperCase(),
                "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        List<Map<String, Object>> files = logService.listFiles();
        return Map.of(
                "success", true,
                "data", Map.of(
                        "status", "UP",
                        "service", "日志管理服务",
                        "logFiles", files.size(),
                        "timestamp", System.currentTimeMillis()
                ),
                "timestamp", System.currentTimeMillis()
        );
    }
}
