package org.example.redis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.example.redis.monitor.RedisHealth;
import org.example.redis.monitor.RedisMetrics;
import org.example.redis.service.*;
import org.example.redis.service.impl.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 统一配置 — 替代旧 {@code RedisConfig}。
 * 提供连接工厂、StringRedisTemplate、所有新服务 bean。
 */
@Slf4j
@Configuration
public class RedisConfiguration {

    @Value("${spring.redis.host:127.0.0.1}")
    private String host;

    @Value("${spring.redis.port:6379}")
    private int port;

    @Value("${spring.redis.password:}")
    private String password;

    @Value("${spring.redis.database:0}")
    private int database;

    @Value("${spring.redis.timeout:5000}")
    private long timeout;

    @Bean
    @ConditionalOnMissingBean(RedisConnectionFactory.class)
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPort(port);
        config.setPassword(password);
        config.setDatabase(database);

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(timeout))
                .clientName("springboot4Demo")
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientConfig);
        factory.afterPropertiesSet();
        log.info("Redis 连接工厂创建完成: {}:{}/{}", host, port, database);
        return factory;
    }

    @Bean
    @ConditionalOnMissingBean(StringRedisTemplate.class)
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public ObjectMapper redisObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .findAndRegisterModules();
    }

    @Bean
    public RedisMetrics redisMetrics() {
        return new RedisMetrics();
    }

    @Bean
    public RedisOps redisOps(StringRedisTemplate stringRedisTemplate) {
        return new RedisOpsImpl(stringRedisTemplate);
    }

    @Bean
    public RedisCache redisCache(RedisOps redisOps, ObjectMapper redisObjectMapper, RedisMetrics redisMetrics) {
        return new RedisCacheImpl(redisOps, redisObjectMapper, redisMetrics);
    }

    @Bean
    public RedisLock redisLock(StringRedisTemplate stringRedisTemplate) {
        return new RedisLockImpl(stringRedisTemplate);
    }

    @Bean
    public RedisRateLimiter redisRateLimiter(StringRedisTemplate stringRedisTemplate) {
        return new RedisRateLimiterImpl(stringRedisTemplate);
    }

    @Bean
    public RedisHealth redisHealth(RedisOps redisOps) {
        return new RedisHealth(redisOps);
    }
}
