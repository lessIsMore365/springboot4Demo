package org.example.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 主配置类
 * 配置HTTP安全规则、密码编码器等
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * 密码编码器 - 使用BCrypt加密
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 客户端密码编码器 - 使用无操作编码（客户端密码明文存储）
     */
    @Bean(name = "clientPasswordEncoder")
    public PasswordEncoder clientPasswordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    /**
     * 认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * 安全过滤器链配置
     */
    @Bean
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
                                "/oauth2/**",             // OAuth2端点
                                "/.well-known/**"         // OpenID Connect配置
                        ).permitAll()
                        // 需要认证的端点
                        .requestMatchers("/api/users/**").authenticated()
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