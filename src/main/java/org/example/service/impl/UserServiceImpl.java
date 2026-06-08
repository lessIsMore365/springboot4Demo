package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.Role;
import org.example.entity.SysDept;
import org.example.entity.User;
import org.example.entity.UserRole;
import org.example.mapper.RoleMapper;
import org.example.mapper.SysDeptMapper;
import org.example.mapper.UserRoleMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.example.mapper.UserMapper;
import org.example.service.UserService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

/**
 * 用户服务实现类
 * 演示虚拟线程环境下的MyBatis Plus操作
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final SysDeptMapper deptMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    public UserServiceImpl(PasswordEncoder passwordEncoder, SysDeptMapper deptMapper,
                           RoleMapper roleMapper, UserRoleMapper userRoleMapper) {
        this.passwordEncoder = passwordEncoder;
        this.deptMapper = deptMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    /**
     * 添加用户 — 根据 deptId 自动赋予部门默认角色（可与传入的特殊角色合并）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addUser(User user) {
        Thread currentThread = Thread.currentThread();
        log.info("添加用户 - 当前线程: {}, 是否虚拟线程: {}", currentThread, currentThread.isVirtual());

        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        // 根据 deptId 自动补齐部门默认角色
        resolveRoles(user);

        boolean result = this.save(user);
        if (result) {
            syncUserRoles(user);
        }
        return result;
    }

    /**
     * 根据 deptId 查询部门默认角色，合并到 user.roles 字段
     */
    private void resolveRoles(User user) {
        Set<String> roleCodes = new HashSet<>();
        // 收集传入的 roles
        if (user.getRoles() != null && !user.getRoles().isBlank()) {
            Arrays.stream(user.getRoles().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(roleCodes::add);
        }

        // 保证基础角色
        roleCodes.add("ROLE_USER");

        // 部门默认角色
        if (user.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(user.getDeptId());
            if (dept != null && dept.getDefaultRoleId() != null) {
                Role deptRole = roleMapper.selectById(dept.getDefaultRoleId());
                if (deptRole != null) {
                    roleCodes.add(deptRole.getCode());
                    log.info("自动赋予部门默认角色: deptId={}, role={}", user.getDeptId(), deptRole.getCode());
                }
            }
        }

        user.setRoles(String.join(",", roleCodes));
    }

    /**
     * 将 user.roles 字符串中的角色同步写入 sys_user_role 表
     */
    private void syncUserRoles(User user) {
        // 先删除已有角色关联，再重新写入（避免唯一约束冲突）
        LambdaQueryWrapper<UserRole> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(UserRole::getUserId, user.getId());
        userRoleMapper.delete(deleteWrapper);

        if (user.getRoles() == null || user.getRoles().isBlank()) return;
        String[] codes = user.getRoles().split(",");
        for (String code : codes) {
            String trimmed = code.trim();
            if (trimmed.isEmpty()) continue;
            Role role = roleMapper.selectOne(
                    new LambdaQueryWrapper<Role>().eq(Role::getCode, trimmed));
            if (role != null) {
                UserRole ur = new UserRole();
                ur.setUserId(user.getId());
                ur.setRoleId(role.getId());
                userRoleMapper.insert(ur);
                log.debug("写入 sys_user_role: userId={}, roleId={}, code={}", user.getId(), role.getId(), trimmed);
            }
        }
    }

    /**
     * 异步添加用户 - 使用虚拟线程
     * 注意：@Async 导致 ThreadLocal 不传播，syncUserRoles 中已使用 Mapper 直接查询实现
     */
    @Async("taskExecutor")
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompletableFuture<Boolean> addUserAsync(User user) {
        Thread currentThread = Thread.currentThread();
        log.info("异步添加用户 - 当前线程: {}, 是否虚拟线程: {}", currentThread, currentThread.isVirtual());

        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        resolveRoles(user);

        boolean result = this.save(user);
        if (result) {
            syncUserRoles(user);
        }
        return CompletableFuture.completedFuture(result);
    }

    /**
     * 批量添加用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchAddUsers(List<User> users) {
        Thread currentThread = Thread.currentThread();
        log.info("批量添加 {} 个用户 - 当前线程: {}, 是否虚拟线程: {}",
                users.size(), currentThread, currentThread.isVirtual());

        return this.saveBatch(users);
    }

    /**
     * 异步批量添加用户 - 使用虚拟线程
     */
    @Async("taskExecutor")
    @Override
    public CompletableFuture<Boolean> batchAddUsersAsync(List<User> users) {
        Thread currentThread = Thread.currentThread();
        log.info("异步批量添加 {} 个用户 - 当前线程: {}, 是否虚拟线程: {}",
                users.size(), currentThread, currentThread.isVirtual());

        boolean result = this.saveBatch(users);
        return CompletableFuture.completedFuture(result);
    }

    /**
     * 分页查询用户
     */
    @Override
    public Page<User> getUsersByPage(int page, int size) {
        Thread currentThread = Thread.currentThread();
        log.info("分页查询用户 - 页码: {}, 大小: {}, 当前线程: {}, 是否虚拟线程: {}",
                page, size, currentThread, currentThread.isVirtual());

        Page<User> pageParam = new Page<>(page, size);
        Page<User> result = this.page(pageParam);

        // 批量填充部门名称
        if (!result.getRecords().isEmpty()) {
            List<Long> deptIds = result.getRecords().stream()
                    .map(User::getDeptId)
                    .filter(id -> id != null)
                    .distinct()
                    .toList();
            if (!deptIds.isEmpty()) {
                List<SysDept> depts = deptMapper.selectBatchIds(deptIds);
                Map<Long, String> deptNameMap = depts.stream()
                        .collect(Collectors.toMap(SysDept::getId, SysDept::getName));
                result.getRecords().forEach(u -> {
                    if (u.getDeptId() != null) {
                        u.setDeptName(deptNameMap.get(u.getDeptId()));
                    }
                });
            }
        }

        return result;
    }

    /**
     * 异步分页查询用户 - 使用虚拟线程
     */
    @Async("taskExecutor")
    @Override
    public CompletableFuture<Page<User>> getUsersByPageAsync(int page, int size) {
        Thread currentThread = Thread.currentThread();
        log.info("异步分页查询用户 - 页码: {}, 大小: {}, 当前线程: {}, 是否虚拟线程: {}",
                page, size, currentThread, currentThread.isVirtual());

        Page<User> result = this.getUsersByPage(page, size);
        return CompletableFuture.completedFuture(result);
    }

    /**
     * 根据用户名查询用户
     */
    @Override
    public List<User> getUsersByUsername(String username) {
        Thread currentThread = Thread.currentThread();
        log.info("根据用户名查询用户 - 用户名: {}, 当前线程: {}, 是否虚拟线程: {}",
                username, currentThread, currentThread.isVirtual());

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        return this.list(queryWrapper);
    }

    /**
     * 统计用户数量
     */
    @Override
    public long countUsers() {
        Thread currentThread = Thread.currentThread();
        log.info("统计用户数量 - 当前线程: {}, 是否虚拟线程: {}", currentThread, currentThread.isVirtual());

        return this.count();
    }

    /**
     * 异步统计用户数量 - 使用虚拟线程
     */
    @Async("taskExecutor")
    @Override
    public CompletableFuture<Long> countUsersAsync() {
        Thread currentThread = Thread.currentThread();
        log.info("异步统计用户数量 - 当前线程: {}, 是否虚拟线程: {}", currentThread, currentThread.isVirtual());

        long count = this.count();
        return CompletableFuture.completedFuture(count);
    }

    /**
     * 测试数据库连接和性能
     */
    @Override
    public String testDatabasePerformance() {
        Thread currentThread = Thread.currentThread();
        log.info("开始数据库性能测试 - 当前线程: {}, 是否虚拟线程: {}", currentThread, currentThread.isVirtual());

        Instant start = Instant.now();

        // 测试1: 统计数量
        long count = this.count();
        log.info("数据库统计测试 - 用户总数: {}", count);

        // 测试2: 分页查询
        Page<User> pageResult = this.getUsersByPage(1, 10);
        log.info("数据库分页测试 - 总记录数: {}, 当前页数据: {}",
                pageResult.getTotal(), pageResult.getRecords().size());

        // 测试3: 插入测试数据
        User testUser = new User();
        testUser.setUsername("test_user_" + System.currentTimeMillis());
        testUser.setEmail("test@example.com");
        testUser.setAge(25);
        testUser.setRemark("性能测试用户");

        boolean insertResult = this.save(testUser);
        log.info("数据库插入测试 - 插入结果: {}, 用户ID: {}", insertResult, testUser.getId());

        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);

        String result = String.format("""
                数据库性能测试完成:
                - 测试线程: %s (虚拟线程: %s)
                - 用户总数: %d
                - 分页查询: %d 条记录
                - 插入测试: %s (ID: %d)
                - 总耗时: %d 毫秒
                - 平均耗时: %.2f 毫秒/操作
                """,
                currentThread.getName(),
                currentThread.isVirtual(),
                count,
                pageResult.getRecords().size(),
                insertResult ? "成功" : "失败",
                testUser.getId(),
                duration.toMillis(),
                duration.toMillis() / 4.0);

        log.info("数据库性能测试结果:\n{}", result);
        return result;
    }

    /**
     * 验证用户密码
     */
    @Override
    public boolean verifyPassword(String username, String password) {
        Thread currentThread = Thread.currentThread();
        log.info("验证用户密码 - 用户名: {}, 当前线程: {}, 是否虚拟线程: {}",
                username, currentThread, currentThread.isVirtual());

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        User user = this.getOne(queryWrapper);

        if (user == null || user.getPassword() == null) {
            return false;
        }

        boolean matches = passwordEncoder.matches(password, user.getPassword());
        log.info("密码验证结果 - 用户名: {}, 结果: {}", username, matches);
        return matches;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(User user) {
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        // 部门变更时重新解析角色
        if (user.getDeptId() != null) {
            User existing = this.getById(user.getId());
            if (existing != null && !user.getDeptId().equals(existing.getDeptId())) {
                resolveRoles(user);
                syncUserRoles(user);
            }
        }

        return this.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUser(Long id) {
        return this.removeById(id);
    }
}