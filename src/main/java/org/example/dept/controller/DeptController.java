package org.example.dept.controller;

import org.example.dept.service.DeptService;
import org.example.entity.SysDept;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dept")
public class DeptController {

    private final DeptService deptService;

    public DeptController(DeptService deptService) {
        this.deptService = deptService;
    }

    /** 部门树 */
    @GetMapping("/tree")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> tree() {
        return Map.of(
                "success", true,
                "data", deptService.getDeptTree(),
                "timestamp", System.currentTimeMillis()
        );
    }

    /** 部门树（排除指定部门，用于上级部门下拉） */
    @GetMapping("/tree/exclude/{deptId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> treeExcluding(@PathVariable Long deptId) {
        return Map.of(
                "success", true,
                "data", deptService.getDeptTreeExcluding(deptId),
                "timestamp", System.currentTimeMillis()
        );
    }

    /** 所有部门平铺列表 */
    @GetMapping("/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> list() {
        return Map.of(
                "success", true,
                "data", deptService.listAll(),
                "timestamp", System.currentTimeMillis()
        );
    }

    /** 新增部门 */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> add(@RequestBody SysDept dept) {
        boolean ok = deptService.addDept(dept);
        return Map.of(
                "success", ok,
                "data", dept,
                "message", ok ? "添加成功" : "添加失败",
                "timestamp", System.currentTimeMillis()
        );
    }

    /** 更新部门 */
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> update(@RequestBody SysDept dept) {
        boolean ok = deptService.updateDept(dept);
        return Map.of(
                "success", ok,
                "message", ok ? "更新成功" : "更新失败",
                "timestamp", System.currentTimeMillis()
        );
    }

    /** 更新部门（RESTful 风格，ID 在路径中） */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody SysDept dept) {
        dept.setId(id);
        boolean ok = deptService.updateDept(dept);
        return Map.of(
                "success", ok,
                "message", ok ? "更新成功" : "更新失败",
                "timestamp", System.currentTimeMillis()
        );
    }

    /** 删除部门 */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> delete(@PathVariable Long id) {
        try {
            boolean ok = deptService.deleteDept(id);
            return Map.of(
                    "success", ok,
                    "message", ok ? "删除成功" : "删除失败",
                    "timestamp", System.currentTimeMillis()
            );
        } catch (IllegalStateException e) {
            return Map.of(
                    "success", false,
                    "message", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            );
        }
    }
}
