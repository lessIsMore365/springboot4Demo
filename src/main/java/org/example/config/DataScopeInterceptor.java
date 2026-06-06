package org.example.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.common.DataScopeHelper;
import org.example.entity.Role;
import org.example.entity.SysDept;
import org.example.entity.User;
import org.example.entity.UserRole;
import org.example.mapper.RoleMapper;
import org.example.mapper.SysDeptMapper;
import org.example.mapper.UserMapper;
import org.example.mapper.UserRoleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.*;

/**
 * 请求拦截器 — preHandle 中装载数据权限上下文
 */
public class DataScopeInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(DataScopeInterceptor.class);

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final SysDeptMapper deptMapper;

    public DataScopeInterceptor(UserMapper userMapper, UserRoleMapper userRoleMapper,
                                RoleMapper roleMapper, SysDeptMapper deptMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
        this.deptMapper = deptMapper;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() ||
                "anonymousUser".equals(auth.getPrincipal())) {
            return true;
        }

        String username = auth.getName();
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            return true;
        }

        int effectiveScope = resolveEffectiveScope(user);
        DataScopeHelper.DataScopeContext ctx = buildContext(effectiveScope, user);
        DataScopeHelper.set(ctx);

        log.debug("Data scope set: username={}, scope={}, deptId={}",
                username, effectiveScope, user.getDeptId());
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler, Exception ex) {
        DataScopeHelper.clear();
    }

    // ======================== private ========================

    private int resolveEffectiveScope(User user) {
        Set<String> roleCodes = new HashSet<>();

        // from User.roles field (comma-separated codes)
        if (user.getRoles() != null && !user.getRoles().isBlank()) {
            for (String code : user.getRoles().split(",")) {
                String trimmed = code.trim();
                if (!trimmed.isEmpty()) {
                    roleCodes.add(trimmed);
                }
            }
        }

        // from sys_user_role table
        var userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId()));
        for (UserRole ur : userRoles) {
            Role r = roleMapper.selectById(ur.getRoleId());
            if (r != null && r.getCode() != null && !r.getCode().isBlank()) {
                roleCodes.add(r.getCode());
            }
        }

        if (roleCodes.isEmpty()) {
            return 5; // no roles → most restrictive
        }

        // load role entities to read data_scope
        var roles = roleMapper.selectList(
                new LambdaQueryWrapper<Role>().in(Role::getCode, roleCodes));

        int effective = 5;
        for (Role r : roles) {
            try {
                int s = Integer.parseInt(r.getDataScope() != null ? r.getDataScope() : "5");
                if (s < effective) effective = s;
            } catch (NumberFormatException ignored) {
            }
        }
        return effective;
    }

    private DataScopeHelper.DataScopeContext buildContext(int scope, User user) {
        Long deptId = user.getDeptId();
        return switch (scope) {
            case 1 -> new DataScopeHelper.DataScopeContext(1, user.getId(), deptId, null);
            case 2, 3 -> {
                if (deptId == null) {
                    yield new DataScopeHelper.DataScopeContext(5, user.getId(), null, null);
                }
                yield new DataScopeHelper.DataScopeContext(3, user.getId(), deptId, null);
            }
            case 4 -> {
                if (deptId == null) {
                    yield new DataScopeHelper.DataScopeContext(5, user.getId(), null, null);
                }
                Set<Long> ids = new HashSet<>();
                ids.add(deptId);
                collectChildDeptIds(deptId, ids);
                yield new DataScopeHelper.DataScopeContext(4, user.getId(), deptId, ids);
            }
            case 5 -> new DataScopeHelper.DataScopeContext(5, user.getId(), deptId, null);
            default -> new DataScopeHelper.DataScopeContext(5, user.getId(), deptId, null);
        };
    }

    private void collectChildDeptIds(Long parentId, Set<Long> collector) {
        var children = deptMapper.selectList(
                new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, parentId));
        for (SysDept child : children) {
            collector.add(child.getId());
            collectChildDeptIds(child.getId(), collector);
        }
    }
}
