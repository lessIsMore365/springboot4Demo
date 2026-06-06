package org.example.menu.controller;

import org.example.entity.SysMenu;
import org.example.menu.service.MenuService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/menus")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    /** 完整菜单树（管理用） */
    @GetMapping("/tree")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> getMenuTree() {
        return Map.of(
                "success", true,
                "data", menuService.getMenuTree(),
                "timestamp", System.currentTimeMillis()
        );
    }

    /** 当前用户可见菜单树 */
    @GetMapping("/user")
    public Map<String, Object> getUserMenus(Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "";
        return Map.of(
                "success", true,
                "data", menuService.getUserMenus(username),
                "timestamp", System.currentTimeMillis()
        );
    }

    /** 获取角色菜单ID列表 */
    @GetMapping("/role/{roleId}")
    public Map<String, Object> getRoleMenuIds(@PathVariable Long roleId) {
        return Map.of(
                "success", true,
                "data", menuService.getMenuIdsByRoleId(roleId),
                "timestamp", System.currentTimeMillis()
        );
    }

    /** 新增菜单 */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> addMenu(@RequestBody SysMenu menu) {
        boolean ok = menuService.addMenu(menu);
        return Map.of(
                "success", ok,
                "data", menu,
                "message", ok ? "菜单添加成功" : "菜单添加失败",
                "timestamp", System.currentTimeMillis()
        );
    }

    /** 更新菜单 */
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> updateMenu(@RequestBody SysMenu menu) {
        boolean ok = menuService.updateMenu(menu);
        return Map.of(
                "success", ok,
                "message", ok ? "菜单更新成功" : "菜单更新失败",
                "timestamp", System.currentTimeMillis()
        );
    }

    /** 删除菜单（级联删除子菜单） */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> deleteMenu(@PathVariable Long id) {
        boolean ok = menuService.deleteMenu(id);
        return Map.of(
                "success", ok,
                "message", ok ? "菜单删除成功" : "菜单删除失败",
                "timestamp", System.currentTimeMillis()
        );
    }

    /** 为角色分配菜单 */
    @PutMapping("/role/{roleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> assignMenusToRole(@PathVariable Long roleId,
                                                  @RequestBody List<Long> menuIds) {
        boolean ok = menuService.assignMenusToRole(roleId, menuIds);
        return Map.of(
                "success", ok,
                "message", ok ? "菜单分配成功" : "菜单分配失败",
                "timestamp", System.currentTimeMillis()
        );
    }

}
