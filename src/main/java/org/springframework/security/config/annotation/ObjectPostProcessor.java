package org.springframework.security.config.annotation;

/**
 * Spring Security 7.x 兼容性桥接类
 * Spring Authorization Server 1.x 引用 ObjectPostProcessor 的旧位置，
 * 但在 Spring Security 7.x 中已移至 org.springframework.security.config.ObjectPostProcessor
 */
@Deprecated
public interface ObjectPostProcessor<T> extends org.springframework.security.config.ObjectPostProcessor<T> {
}
