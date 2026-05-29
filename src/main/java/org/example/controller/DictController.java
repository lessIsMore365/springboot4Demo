package org.example.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.entity.SysDictData;
import org.example.entity.SysDictType;
import org.example.service.DictService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/dict")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    // ====== 字典类型 ======

    @GetMapping("/type")
    public Map<String, Object> listTypes(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        Page<SysDictType> typePage = dictService.getTypesByPage(page, size);
        return Map.of("success", true, "data", typePage.getRecords(),
                "pagination", Map.of("page", typePage.getCurrent(), "size", typePage.getSize(),
                        "total", typePage.getTotal(), "pages", typePage.getPages()),
                "timestamp", System.currentTimeMillis());
    }

    @GetMapping("/type/all")
    public Map<String, Object> allTypes() {
        return Map.of("success", true, "data", dictService.getAllTypes(),
                "timestamp", System.currentTimeMillis());
    }

    @GetMapping("/type/{id}")
    public Map<String, Object> getType(@PathVariable Long id) {
        SysDictType type = dictService.getTypeById(id);
        return type == null
                ? Map.of("success", false, "message", "字典类型不存在", "timestamp", System.currentTimeMillis())
                : Map.of("success", true, "data", type, "timestamp", System.currentTimeMillis());
    }

    @PostMapping("/type")
    public Map<String, Object> createType(@RequestBody SysDictType dictType) {
        dictService.saveType(dictType);
        dictService.refreshCache();
        return Map.of("success", true, "message", "字典类型创建成功", "timestamp", System.currentTimeMillis());
    }

    @PutMapping("/type")
    public Map<String, Object> updateType(@RequestBody SysDictType dictType) {
        dictService.updateType(dictType);
        return Map.of("success", true, "message", "字典类型更新成功", "timestamp", System.currentTimeMillis());
    }

    @DeleteMapping("/type/{id}")
    public Map<String, Object> deleteType(@PathVariable Long id) {
        dictService.deleteType(id);
        return Map.of("success", true, "message", "字典类型已删除（含字典数据）", "timestamp", System.currentTimeMillis());
    }

    @PostMapping("/refresh-cache")
    public Map<String, Object> refreshCache() {
        dictService.refreshCache();
        return Map.of("success", true, "message", "字典缓存刷新成功", "timestamp", System.currentTimeMillis());
    }

    // ====== 字典数据 ======

    @GetMapping("/data")
    public Map<String, Object> listData(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size,
                                        @RequestParam(required = false) String dictType) {
        Page<SysDictData> dataPage = dictService.getDataByPage(page, size, dictType);
        return Map.of("success", true, "data", dataPage.getRecords(),
                "pagination", Map.of("page", dataPage.getCurrent(), "size", dataPage.getSize(),
                        "total", dataPage.getTotal(), "pages", dataPage.getPages()),
                "timestamp", System.currentTimeMillis());
    }

    /** 按字典类型获取字典数据列表（走 Redis 缓存） */
    @GetMapping("/data/type/{dictType}")
    public Map<String, Object> getDataByType(@PathVariable String dictType) {
        List<SysDictData> list = dictService.getDataByType(dictType);
        return Map.of("success", true, "data", list, "timestamp", System.currentTimeMillis());
    }

    @GetMapping("/data/{id}")
    public Map<String, Object> getData(@PathVariable Long id) {
        SysDictData data = dictService.getDataById(id);
        return data == null
                ? Map.of("success", false, "message", "字典数据不存在", "timestamp", System.currentTimeMillis())
                : Map.of("success", true, "data", data, "timestamp", System.currentTimeMillis());
    }

    @PostMapping("/data")
    public Map<String, Object> createData(@RequestBody SysDictData dictData) {
        dictService.saveData(dictData);
        return Map.of("success", true, "message", "字典数据创建成功", "timestamp", System.currentTimeMillis());
    }

    @PutMapping("/data")
    public Map<String, Object> updateData(@RequestBody SysDictData dictData) {
        dictService.updateData(dictData);
        return Map.of("success", true, "message", "字典数据更新成功", "timestamp", System.currentTimeMillis());
    }

    @DeleteMapping("/data/{id}")
    public Map<String, Object> deleteData(@PathVariable Long id) {
        dictService.deleteData(id);
        return Map.of("success", true, "message", "字典数据已删除", "timestamp", System.currentTimeMillis());
    }
}
