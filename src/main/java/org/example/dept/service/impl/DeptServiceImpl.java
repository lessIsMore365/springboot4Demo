package org.example.dept.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.example.dept.service.DeptService;
import org.example.entity.SysDept;
import org.example.mapper.SysDeptMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements DeptService {

    private final SysDeptMapper deptMapper;

    public DeptServiceImpl(SysDeptMapper deptMapper) {
        this.deptMapper = deptMapper;
    }

    @Override
    public List<DeptNode> getDeptTree() {
        List<SysDept> all = deptMapper.selectList(
                new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getSortOrder));
        return buildTree(all);
    }

    @Override
    public List<DeptNode> getDeptTreeExcluding(Long deptId) {
        Set<Long> excludeIds = collectChildIds(deptId);
        excludeIds.add(deptId);
        List<SysDept> all = deptMapper.selectList(
                new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getSortOrder));
        return buildTree(all.stream()
                .filter(d -> !excludeIds.contains(d.getId()))
                .toList());
    }

    @Override
    public List<SysDept> listAll() {
        return deptMapper.selectList(
                new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getSortOrder));
    }

    @Override
    public boolean addDept(SysDept dept) {
        return this.save(dept);
    }

    @Override
    public boolean updateDept(SysDept dept) {
        return this.updateById(dept);
    }

    @Override
    public boolean deleteDept(Long id) {
        long children = deptMapper.selectCount(
                new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id));
        if (children > 0) {
            throw new IllegalStateException("存在子部门，无法删除");
        }
        return this.removeById(id);
    }

    // ======================== private ========================

    private List<DeptNode> buildTree(List<SysDept> list) {
        if (CollectionUtils.isEmpty(list)) return Collections.emptyList();

        Map<Long, List<SysDept>> childrenMap = list.stream()
                .collect(Collectors.groupingBy(SysDept::getParentId));

        return buildChildren(0L, childrenMap);
    }

    private List<DeptNode> buildChildren(Long parentId, Map<Long, List<SysDept>> map) {
        List<SysDept> children = map.getOrDefault(parentId, Collections.emptyList());
        return children.stream()
                .map(d -> new DeptNode(
                        d.getId(), d.getParentId(), d.getName(), d.getSortOrder(),
                        d.getLeader(), d.getPhone(), d.getEmail(),
                        d.getStatus() == 0,
                        buildChildren(d.getId(), map)))
                .toList();
    }

    private Set<Long> collectChildIds(Long parentId) {
        Set<Long> ids = new HashSet<>();
        List<SysDept> children = deptMapper.selectList(
                new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, parentId));
        for (SysDept child : children) {
            ids.add(child.getId());
            ids.addAll(collectChildIds(child.getId()));
        }
        return ids;
    }
}
