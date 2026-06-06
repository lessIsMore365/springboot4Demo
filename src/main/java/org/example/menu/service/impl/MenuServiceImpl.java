package org.example.menu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.SysMenu;
import org.example.entity.SysRoleMenu;
import org.example.entity.User;
import org.example.entity.UserRole;
import org.example.mapper.SysMenuMapper;
import org.example.mapper.SysRoleMenuMapper;
import org.example.mapper.UserMapper;
import org.example.mapper.UserRoleMapper;
import org.example.menu.service.MenuService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements MenuService {

    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserMapper userMapper;

    @Override
    public List<MenuNode> getMenuTree() {
        List<SysMenu> allMenus = menuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>()
                        .eq(SysMenu::getStatus, 0)
                        .orderByAsc(SysMenu::getSortOrder));
        return buildTree(allMenus);
    }

    @Override
    public List<MenuNode> getUserMenus(String username) {
        // 0. 根据用户名查找用户ID
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) return Collections.emptyList();

        Long userId = user.getId();

        // 1. 获取用户的所有角色
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (CollectionUtils.isEmpty(userRoles)) return Collections.emptyList();

        List<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).toList();

        // 2. 获取这些角色分配的菜单ID
        List<SysRoleMenu> roleMenus = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, roleIds));

        if (CollectionUtils.isEmpty(roleMenus)) return Collections.emptyList();

        Set<Long> menuIds = roleMenus.stream().map(SysRoleMenu::getMenuId).collect(Collectors.toSet());

        // 3. 获取可见菜单并构建树
        List<SysMenu> menus = menuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>()
                        .in(SysMenu::getId, menuIds)
                        .eq(SysMenu::getStatus, 0)
                        .eq(SysMenu::getVisible, 0)
                        .orderByAsc(SysMenu::getSortOrder));

        return buildTree(menus);
    }

    @Override
    public List<Long> getMenuIdsByRoleId(Long roleId) {
        return roleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId))
                .stream().map(SysRoleMenu::getMenuId).toList();
    }

    @Override
    public boolean addMenu(SysMenu menu) {
        return this.save(menu);
    }

    @Override
    public boolean updateMenu(SysMenu menu) {
        return this.updateById(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteMenu(Long id) {
        // 级联删除子菜单
        List<Long> idsToDelete = collectChildIds(id);
        idsToDelete.add(id);

        // 删除角色菜单关联
        for (Long menuId : idsToDelete) {
            roleMenuMapper.delete(
                    new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getMenuId, menuId));
        }

        return this.removeByIds(idsToDelete);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignMenusToRole(Long roleId, List<Long> menuIds) {
        // 删除角色现有的菜单关联
        roleMenuMapper.delete(
                new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));

        if (CollectionUtils.isEmpty(menuIds)) return true;

        // 插入新的菜单关联
        List<SysRoleMenu> roleMenus = menuIds.stream().map(menuId -> {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            return rm;
        }).toList();

        for (SysRoleMenu rm : roleMenus) {
            roleMenuMapper.insert(rm);
        }
        return true;
    }

    // ======================== private ========================

    private List<MenuNode> buildTree(List<SysMenu> menus) {
        if (CollectionUtils.isEmpty(menus)) return Collections.emptyList();

        Map<Long, List<SysMenu>> childrenMap = menus.stream()
                .collect(Collectors.groupingBy(SysMenu::getParentId));

        return buildChildren(0L, childrenMap);
    }

    private List<MenuNode> buildChildren(Long parentId, Map<Long, List<SysMenu>> childrenMap) {
        List<SysMenu> children = childrenMap.getOrDefault(parentId, Collections.emptyList());
        return children.stream()
                .map(m -> new MenuNode(
                        m.getId(), m.getParentId(), m.getName(), m.getPath(),
                        m.getComponent(), m.getIcon(), m.getSortOrder(),
                        m.getMenuType(), m.getPermission(),
                        m.getVisible() == 0, m.getStatus() == 0,
                        buildChildren(m.getId(), childrenMap)))
                .toList();
    }

    private List<Long> collectChildIds(Long parentId) {
        List<Long> childIds = new ArrayList<>();
        List<SysMenu> children = menuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, parentId));
        for (SysMenu child : children) {
            childIds.add(child.getId());
            childIds.addAll(collectChildIds(child.getId()));
        }
        return childIds;
    }
}
