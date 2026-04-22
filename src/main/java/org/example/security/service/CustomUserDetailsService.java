package org.example.security.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.User;
import org.example.mapper.UserMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * 自定义UserDetailsService
 * 从数据库加载用户信息，实现Spring Security的用户详情服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    /**
     * 根据用户名加载用户
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Thread currentThread = Thread.currentThread();
        log.info("加载用户详情 - 用户名: {}, 当前线程: {}, 是否虚拟线程: {}",
                username, currentThread, currentThread.isVirtual());

        // 查询用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(queryWrapper);

        if (user == null) {
            log.error("用户不存在: {}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 更新最后登录时间
        updateLastLoginTime(user);

        log.info("用户加载成功: {}, 角色: {}", username, user.getRoles());
        return createUserDetails(user);
    }

    /**
     * 创建Spring Security UserDetails对象
     */
    private UserDetails createUserDetails(User user) {
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                isEnabled(user),
                isAccountNonExpired(user),
                isCredentialsNonExpired(user),
                isAccountNonLocked(user),
                getAuthorities(user)
        );
    }

    /**
     * 获取用户权限
     */
    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        if (user.getRoles() == null || user.getRoles().trim().isEmpty()) {
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return Arrays.stream(user.getRoles().split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    /**
     * 检查账户是否启用
     */
    private boolean isEnabled(User user) {
        return user.getEnabled() != null && user.getEnabled();
    }

    /**
     * 检查账户是否未过期
     */
    private boolean isAccountNonExpired(User user) {
        return user.getAccountNonExpired() != null && user.getAccountNonExpired();
    }

    /**
     * 检查凭证是否未过期
     */
    private boolean isCredentialsNonExpired(User user) {
        return user.getCredentialsNonExpired() != null && user.getCredentialsNonExpired();
    }

    /**
     * 检查账户是否未锁定
     */
    private boolean isAccountNonLocked(User user) {
        return user.getAccountNonLocked() != null && user.getAccountNonLocked();
    }

    /**
     * 更新最后登录时间
     */
    @Transactional
    public void updateLastLoginTime(User user) {
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);
        log.debug("更新用户最后登录时间: {}, 时间: {}", user.getUsername(), user.getLastLoginTime());
    }

    /**
     * 根据用户ID加载用户
     */
    public UserDetails loadUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在，ID: " + userId);
        }
        return createUserDetails(user);
    }
}