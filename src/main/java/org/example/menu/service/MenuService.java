package org.example.menu.service;

import org.example.entity.SysMenu;

import java.util.List;

public interface MenuService {

    /** 菜单树节点 */
    record MenuNode(Long id, Long parentId, String name, String path,
                    String component, String icon, Integer sortOrder,
                    String menuType, String permission, boolean visible,
                    boolean status, List<MenuNode> children) {}

    /** 获取完整菜单树（管理用） */
    List<MenuNode> getMenuTree();

    /** 获取当前用户可见的菜单树 */
    List<MenuNode> getUserMenus(String username);

    /** 获取角色分配的菜单ID列表 */
    List<Long> getMenuIdsByRoleId(Long roleId);

    /** 新增菜单 */
    boolean addMenu(SysMenu menu);

    /** 更新菜单 */
    boolean updateMenu(SysMenu menu);

    /** 删除菜单（级联删除子菜单） */
    boolean deleteMenu(Long id);

    /** 为角色分配菜单 */
    boolean assignMenusToRole(Long roleId, List<Long> menuIds);
}
