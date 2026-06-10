package org.example.dto;

import java.util.Map;

/**
 * 统一 API 响应格式
 * <pre>{@code
 * ApiResponse.ok(data)                          → {"success":true, "data":..., "timestamp":...}
 * ApiResponse.ok(data, "创建成功")              → {"success":true, "data":..., "message":"创建成功", ...}
 * ApiResponse.fail("参数错误")                  → {"success":false, "message":"参数错误", ...}
 * ApiResponse.of(true, "操作成功")              → {"success":true, "message":"操作成功", ...}
 * }</pre>
 */
public record ApiResponse<T>(boolean success, T data, String message, long timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, System.currentTimeMillis());
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, System.currentTimeMillis());
    }

    public static ApiResponse<Void> fail(String message) {
        return new ApiResponse<>(false, null, message, System.currentTimeMillis());
    }

    public static ApiResponse<Void> of(boolean success, String message) {
        return new ApiResponse<>(success, null, message, System.currentTimeMillis());
    }

    public Map<String, Object> toMap() {
        return message != null
                ? Map.of("success", success, "data", data != null ? data : Map.of(),
                          "message", message, "timestamp", timestamp)
                : Map.of("success", success, "data", data != null ? data : Map.of(),
                          "timestamp", timestamp);
    }
}
