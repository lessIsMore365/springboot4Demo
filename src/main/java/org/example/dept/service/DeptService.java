package org.example.dept.service;

import org.example.entity.SysDept;

import java.util.List;

public interface DeptService {

    /** 部门树节点 */
    record DeptNode(Long id, Long parentId, String name, Integer sortOrder,
                    String leader, String phone, String email,
                    boolean status, List<DeptNode> children) {}

    /** 获取完整部门树 */
    List<DeptNode> getDeptTree();

    /** 获取部门树（排除指定部门及其子部门，用于上级部门选择） */
    List<DeptNode> getDeptTreeExcluding(Long deptId);

    /** 获取所有部门（平铺列表） */
    List<SysDept> listAll();

    /** 新增部门 */
    boolean addDept(SysDept dept);

    /** 更新部门 */
    boolean updateDept(SysDept dept);

    /** 删除部门（有子部门则拒绝） */
    boolean deleteDept(Long id);
}
