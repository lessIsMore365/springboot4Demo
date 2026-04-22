package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.RegisterRequest;
import org.example.dto.UserResponse;
import org.example.entity.User;
import org.example.mapper.UserMapper;
import org.example.security.service.CustomUserDetailsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 认证控制器
 * 处理用户注册和获取当前用户信息
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final CustomUserDetailsService userDetailsService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        Thread currentThread = Thread.currentThread();
        log.info("用户注册请求 - 用户名: {}, 当前线程: {}, 是否虚拟线程: {}",
                request.getUsername(), currentThread, currentThread.isVirtual());

        // 检查用户名是否已存在
        var existingUser = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getUsername, request.getUsername())
        );
        if (existingUser != null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", "用户名已存在",
                            "timestamp", System.currentTimeMillis()
                    ));
        }

        // 创建用户实体
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setAge(request.getAge());
        user.setRemark(request.getRemark());
        user.setRoles(request.getRoles());
        user.setEnabled(true);
        user.setAccountNonLocked(true);
        user.setAccountNonExpired(true);
        user.setCredentialsNonExpired(true);
        user.setLastLoginTime(LocalDateTime.now());

        // 保存用户
        int result = userMapper.insert(user);

        if (result > 0) {
            log.info("用户注册成功 - 用户名: {}, 用户ID: {}", request.getUsername(), user.getId());
            return ResponseEntity.ok()
                    .body(Map.of(
                            "success", true,
                            "message", "用户注册成功",
                            "userId", user.getId(),
                            "username", user.getUsername(),
                            "timestamp", System.currentTimeMillis()
                    ));
        } else {
            log.error("用户注册失败 - 用户名: {}", request.getUsername());
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "success", false,
                            "message", "用户注册失败",
                            "timestamp", System.currentTimeMillis()
                    ));
        }
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(Map.of(
                            "success", false,
                            "message", "用户未认证",
                            "timestamp", System.currentTimeMillis()
                    ));
        }

        String username = authentication.getName();
        log.info("获取当前用户信息 - 用户名: {}, 权限: {}", username, authentication.getAuthorities());

        // 查询用户信息
        var user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
        );

        if (user == null) {
            return ResponseEntity.status(404)
                    .body(Map.of(
                            "success", false,
                            "message", "用户不存在",
                            "timestamp", System.currentTimeMillis()
                    ));
        }

        // 转换为响应DTO
        UserResponse userResponse = new UserResponse();
        userResponse.setId(user.getId());
        userResponse.setUsername(user.getUsername());
        userResponse.setEmail(user.getEmail());
        userResponse.setAge(user.getAge());
        userResponse.setRoles(user.getRoles());
        userResponse.setEnabled(user.getEnabled());
        userResponse.setAccountNonLocked(user.getAccountNonLocked());
        userResponse.setAccountNonExpired(user.getAccountNonExpired());
        userResponse.setCredentialsNonExpired(user.getCredentialsNonExpired());
        userResponse.setCreateTime(user.getCreateTime());
        userResponse.setUpdateTime(user.getUpdateTime());
        userResponse.setLastLoginTime(user.getLastLoginTime());
        userResponse.setRemark(user.getRemark());

        return ResponseEntity.ok()
                .body(Map.of(
                        "success", true,
                        "user", userResponse,
                        "authorities", authentication.getAuthorities(),
                        "timestamp", System.currentTimeMillis()
                ));
    }

    /**
     * 健康检查端点（公开）
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok()
                .body(Map.of(
                        "status", "UP",
                        "service", "authentication",
                        "timestamp", System.currentTimeMillis()
                ));
    }
}