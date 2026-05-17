package org.example.java21demo.model;

/**
 * 天气查询结果 - 用于 Structured Concurrency ShutdownOnSuccess 演示
 */
public record WeatherResult(String source, String city, int temperature, String condition) {
}
