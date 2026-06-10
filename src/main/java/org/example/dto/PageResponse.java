package org.example.dto;

import java.util.List;

/**
 * 分页响应
 */
public record PageResponse<T>(List<T> records, long page, long size, long total, long pages) {

    public static <T> PageResponse<T> of(List<T> records, long page, long size, long total) {
        long pages = size > 0 ? (total + size - 1) / size : 0;
        return new PageResponse<>(records, page, size, total, pages);
    }
}
