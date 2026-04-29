package org.example.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.example.security.filter.CaptchaValidationFilter;

/**
 * Spring Security 主配置类
 * 配置HTTP安全规则、密码编码器等
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CaptchaValidationFilter captchaValidationFilter;

    public SecurityConfig(CaptchaValidationFilter captchaValidationFilter) {
        this.captchaValidationFilter = captchaValidationFilter;
    }

    /**
     * 密码编码器 - 使用BCrypt加密
     */
    @Primary
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    /**
     * 认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * OAuth2端点安全过滤器链
     * 禁用HTTP Basic认证，让OAuth2授权服务器处理客户端认证
     */
    @Bean
    @Order(1)
    public SecurityFilterChain oauth2EndpointSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/oauth2/**")
                // 禁用CSRF（REST API无状态）
                .csrf(AbstractHttpConfigurer::disable)
                // 禁用Session（使用无状态认证）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 配置HTTP请求授权
                .authorizeHttpRequests(authorize -> authorize
                        // OAuth2端点由授权服务器处理认证
                        .anyRequest().permitAll()
                )
                // 禁用HTTP Basic认证
                .httpBasic(AbstractHttpConfigurer::disable)
                // 添加验证码验证过滤器（在UsernamePasswordAuthenticationFilter之前执行）
                .addFilterBefore(captchaValidationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * 主安全过滤器链配置（处理其他所有端点）
     */
    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // 禁用CSRF（REST API无状态）
                .csrf(AbstractHttpConfigurer::disable)
                // 禁用Session（使用无状态认证）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 配置HTTP请求授权
                .authorizeHttpRequests(authorize -> authorize
                        // 公开端点
                        .requestMatchers(
                                "/api/auth/**",           // 认证相关端点（注册、健康检查等）
                                "/api/roles/health",      // 角色服务健康检查
                                "/api/permissions/health", // 权限服务健康检查
                                "/api/redis/**",          // Redis 服务端点
                                "/hello",                 // Hello端点
                                "/demo/hello",            // 虚拟线程演示端点
                                "/db/**",                 // 数据库测试端点
                                "/.well-known/**"         // OpenID Connect配置
                        ).permitAll()
                        // 需要认证的端点
                        .requestMatchers("/api/users/**").authenticated()
                        .requestMatchers("/api/roles/**").authenticated()
                        .requestMatchers("/api/permissions/**").authenticated()
                        // 其他请求需要认证
                        .anyRequest().authenticated()
                )
                // 启用HTTP Basic认证（用于测试）
                .httpBasic(Customizer.withDefaults())
                // 启用OAuth2资源服务器
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                )
                .build();
    }
}