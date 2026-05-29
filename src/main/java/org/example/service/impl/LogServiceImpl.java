package org.example.service.impl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.service.LogService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class LogServiceImpl implements LogService {

    private static final DateTimeFormatter LOG_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    // 兼容日志时间戳格式（空格分隔，毫秒可能缺失）
    private static final Pattern LOG_TIME_PATTERN = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,6})?)"
    );
    private static final Pattern LEVEL_PATTERN = Pattern.compile(
            "\\[(?:[^]]*)\\]\\s+(TRACE|DEBUG|INFO|WARN|ERROR)\\s+"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".log", ".tmp");
    private static final int MAX_SEARCH_LINES = 50000;

    @Value("${logging.file.path:logs}")
    private String logDir;

    private Path logPath;

    @PostConstruct
    public void init() {
        logPath = Paths.get(logDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(logPath);
        } catch (IOException e) {
            log.error("无法创建日志目录: {}", logPath, e);
        }
        log.info("日志管理服务初始化完成 - 日志目录: {}", logPath);
    }

    @Override
    public List<String> tail(int lines, String level, String file) {
        Path filePath = resolveLogFile(file);
        if (!Files.exists(filePath)) {
            return List.of("[日志文件不存在: " + filePath.getFileName() + "]");
        }

        int maxLines = Math.min(lines, 2000);
        try {
            List<String> allLines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            List<String> result = new ArrayList<>();
            int start = Math.max(0, allLines.size() - maxLines);
            for (int i = start; i < allLines.size(); i++) {
                String line = allLines.get(i);
                if (matchLevel(line, level)) {
                    result.add(line);
                }
            }
            return result;
        } catch (IOException e) {
            log.error("读取日志文件失败: {}", filePath, e);
            return List.of("[读取日志失败: " + e.getMessage() + "]");
        }
    }

    @Override
    public Map<String, Object> search(String keyword, String level, LocalDateTime from, LocalDateTime to,
                                       int page, int size, String file) {
        Path filePath = resolveLogFile(file);
        if (!Files.exists(filePath)) {
            return Map.of("records", List.of(), "total", 0, "page", page, "size", size,
                    "message", "日志文件不存在: " + filePath.getFileName());
        }

        int maxSize = Math.min(size, 100);
        List<String> matched = new ArrayList<>();
        int scanned = 0;

        try (Stream<String> stream = Files.lines(filePath, StandardCharsets.UTF_8)) {
            Iterator<String> it = stream.iterator();
            while (it.hasNext() && scanned < MAX_SEARCH_LINES) {
                String line = it.next();
                scanned++;
                if (matchFilters(line, keyword, level, from, to)) {
                    matched.add(line);
                }
            }
        } catch (IOException e) {
            log.error("搜索日志文件失败: {}", filePath, e);
            return Map.of("records", List.of(), "total", 0, "page", page, "size", size,
                    "message", "搜索日志失败: " + e.getMessage());
        }

        int total = matched.size();
        int fromIndex = (page - 1) * maxSize;
        int toIndex = Math.min(fromIndex + maxSize, total);
        List<String> pageRecords = fromIndex < total ? matched.subList(fromIndex, toIndex) : List.of();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", pageRecords);
        result.put("total", total);
        result.put("page", page);
        result.put("size", maxSize);
        result.put("scannedLines", scanned);
        result.put("truncated", scanned >= MAX_SEARCH_LINES);
        return result;
    }

    @Override
    public List<Map<String, Object>> listFiles() {
        if (!Files.exists(logPath)) {
            return List.of();
        }
        try (var dirStream = Files.newDirectoryStream(logPath, "*.log*")) {
            List<Map<String, Object>> files = new ArrayList<>();
            for (Path p : dirStream) {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("name", p.getFileName().toString());
                info.put("size", p.toFile().length());
                info.put("sizeDisplay", formatSize(p.toFile().length()));
                long lastMod = Files.getLastModifiedTime(p).toMillis();
                info.put("lastModified", lastMod);
                info.put("lastModifiedDisplay", Files.getLastModifiedTime(p).toString());
                files.add(info);
            }
            files.sort((a, b) -> Long.compare(
                    (Long) b.get("lastModified"),
                    (Long) a.get("lastModified")));
            return files;
        } catch (IOException e) {
            log.error("列出日志文件失败", e);
            return List.of();
        }
    }

    @Override
    public Resource download(String file) {
        Path filePath = resolveLogFile(file);
        if (!Files.exists(filePath)) {
            throw new RuntimeException("日志文件不存在: " + file);
        }
        return new FileSystemResource(filePath);
    }

    @Override
    public Map<String, String> getLoggers() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Map<String, String> result = new TreeMap<>();
        for (var logger : context.getLoggerList()) {
            if (logger.getLevel() != null) {
                result.put(logger.getName(), logger.getLevel().toString());
            }
        }
        return result;
    }

    @Override
    public void setLogLevel(String loggerName, String level) {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Level newLevel = Level.toLevel(level.toUpperCase(), null);
        if (newLevel == null) {
            throw new IllegalArgumentException("无效的日志级别: " + level + "，支持: TRACE, DEBUG, INFO, WARN, ERROR, OFF");
        }
        if ("ROOT".equalsIgnoreCase(loggerName)) {
            context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).setLevel(newLevel);
        } else {
            context.getLogger(loggerName).setLevel(newLevel);
        }
        log.info("日志级别已修改 - logger: {}, level: {}", loggerName, newLevel);
    }

    // ==================== 内部方法 ====================

    private Path resolveLogFile(String file) {
        String fileName = (file == null || file.isBlank()) ? "application.log" : file;
        // 路径穿越防护
        String safeName = Paths.get(fileName).getFileName().toString();
        String ext = safeName.contains(".") ? safeName.substring(safeName.lastIndexOf('.')) : "";
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("不允许的文件类型: " + safeName);
        }
        return logPath.resolve(safeName);
    }

    private boolean matchFilters(String line, String keyword, String level,
                                  LocalDateTime from, LocalDateTime to) {
        if (!matchKeyword(line, keyword)) return false;
        if (!matchLevel(line, level)) return false;
        if (!matchTimeRange(line, from, to)) return false;
        return true;
    }

    private boolean matchKeyword(String line, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        return line.toLowerCase().contains(keyword.toLowerCase());
    }

    private boolean matchLevel(String line, String level) {
        if (level == null || level.isBlank()) return true;
        var matcher = LEVEL_PATTERN.matcher(line);
        return matcher.find() && matcher.group(1).equalsIgnoreCase(level.trim());
    }

    private boolean matchTimeRange(String line, LocalDateTime from, LocalDateTime to) {
        if (from == null && to == null) return true;
        var matcher = LOG_TIME_PATTERN.matcher(line);
        if (!matcher.find()) return true; // 如果无法解析时间，默认包含
        try {
            String timeStr = matcher.group(1).replace(" ", "T");
            if (timeStr.length() == 19) timeStr += ".000"; // 补齐毫秒
            else if (timeStr.length() > 23) timeStr = timeStr.substring(0, 23); // 截断到毫秒
            LocalDateTime logTime = LocalDateTime.parse(timeStr);
            if (from != null && logTime.isBefore(from)) return false;
            if (to != null && logTime.isAfter(to)) return false;
            return true;
        } catch (DateTimeParseException e) {
            return true; // 解析失败，默认包含
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
