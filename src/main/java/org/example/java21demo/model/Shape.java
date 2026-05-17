package org.example.java21demo.model;

/**
 * 图形密封接口 - 演示 sealed class + record pattern matching
 * 只允许 Circle、Rectangle、Triangle 三种实现
 */
public sealed interface Shape permits Circle, Rectangle, Triangle {
}
