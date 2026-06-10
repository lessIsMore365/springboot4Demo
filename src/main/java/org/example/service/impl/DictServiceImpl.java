package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.SysDictData;
import org.example.entity.SysDictType;
import org.example.mapper.SysDictDataMapper;
import org.example.mapper.SysDictTypeMapper;
import org.example.redis.core.RedisKeyNamespace;
import org.example.redis.service.RedisCache;
import org.example.service.DictService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DictServiceImpl implements DictService {

    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictDataMapper dictDataMapper;
    private final RedisCache redisCache;

    private static final Duration CACHE_TTL = Duration.ofHours(24);

    @PostConstruct
    void initCache() {
        refreshCache();
    }

    // ====== 字典类型 ======

    @Override
    public Page<SysDictType> getTypesByPage(int page, int size) {
        LambdaQueryWrapper<SysDictType> w = new LambdaQueryWrapper<>();
        w.orderByAsc(SysDictType::getDictType);
        return dictTypeMapper.selectPage(Page.of(page, size), w);
    }

    @Override
    public List<SysDictType> getAllTypes() {
        return dictTypeMapper.selectList(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getStatus, "0")
                .orderByAsc(SysDictType::getDictType));
    }

    @Override
    public SysDictType getTypeById(Long id) {
        return dictTypeMapper.selectById(id);
    }

    @Override
    public void saveType(SysDictType dictType) {
        dictTypeMapper.insert(dictType);
    }

    @Override
    public void updateType(SysDictType dictType) {
        dictTypeMapper.updateById(dictType);
        refreshCache();
    }

    @Override
    public void deleteType(Long id) {
        SysDictType type = dictTypeMapper.selectById(id);
        if (type != null) {
            dictDataMapper.delete(new LambdaQueryWrapper<SysDictData>()
                    .eq(SysDictData::getDictType, type.getDictType()));
            dictTypeMapper.deleteById(id);
            redisCache.evict(RedisKeyNamespace.SYS_DICT, type.getDictType());
        }
    }

    // ====== 字典数据 ======

    @Override
    public Page<SysDictData> getDataByPage(int page, int size, String dictType) {
        LambdaQueryWrapper<SysDictData> w = new LambdaQueryWrapper<>();
        if (dictType != null && !dictType.isBlank())
            w.eq(SysDictData::getDictType, dictType);
        w.orderByAsc(SysDictData::getDictSort);
        return dictDataMapper.selectPage(Page.of(page, size), w);
    }

    @Override
    public List<SysDictData> getDataByType(String dictType) {
        // Cache-Aside: 优先缓存，miss 时回源 DB
        List<SysDictData> cached = redisCache.getList(RedisKeyNamespace.SYS_DICT, dictType, SysDictData.class);
        if (!cached.isEmpty()) {
            return cached;
        }
        List<SysDictData> list = dictDataMapper.selectList(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getStatus, "0")
                .orderByAsc(SysDictData::getDictSort));
        cacheDictData(dictType, list);
        return list;
    }

    @Override
    public SysDictData getDataById(Long id) {
        return dictDataMapper.selectById(id);
    }

    @Override
    public void saveData(SysDictData dictData) {
        dictDataMapper.insert(dictData);
        refreshCache(dictData.getDictType());
    }

    @Override
    public void updateData(SysDictData dictData) {
        dictDataMapper.updateById(dictData);
        refreshCache(dictData.getDictType());
    }

    @Override
    public void deleteData(Long id) {
        SysDictData data = dictDataMapper.selectById(id);
        if (data != null) {
            dictDataMapper.deleteById(id);
            refreshCache(data.getDictType());
        }
    }

    @Override
    public void refreshCache() {
        List<SysDictType> types = dictTypeMapper.selectList(new LambdaQueryWrapper<SysDictType>()
                .eq(SysDictType::getStatus, "0"));
        for (SysDictType type : types) {
            refreshCache(type.getDictType());
        }
        log.info("字典缓存已刷新，共 {} 个类型", types.size());
    }

    private void refreshCache(String dictType) {
        List<SysDictData> list = dictDataMapper.selectList(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getStatus, "0")
                .orderByAsc(SysDictData::getDictSort));
        cacheDictData(dictType, list);
    }

    private void cacheDictData(String dictType, List<SysDictData> list) {
        redisCache.evict(RedisKeyNamespace.SYS_DICT, dictType);
        if (list.isEmpty()) return;
        redisCache.putAll(RedisKeyNamespace.SYS_DICT, dictType, list, CACHE_TTL);
    }
}
