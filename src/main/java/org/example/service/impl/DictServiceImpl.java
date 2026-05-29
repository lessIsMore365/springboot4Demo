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
import org.example.service.DictService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DictServiceImpl implements DictService {

    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictDataMapper dictDataMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String CACHE_PREFIX = "sys_dict:";
    private static final long CACHE_TTL = 24;
    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

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
            // 级联删除字典数据
            dictDataMapper.delete(new LambdaQueryWrapper<SysDictData>()
                    .eq(SysDictData::getDictType, type.getDictType()));
            dictTypeMapper.deleteById(id);
            stringRedisTemplate.delete(CACHE_PREFIX + type.getDictType());
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
        // 优先从 Redis 缓存读取
        String cacheKey = CACHE_PREFIX + dictType;
        List<String> cached = stringRedisTemplate.opsForList().range(cacheKey, 0, -1);
        if (cached != null && !cached.isEmpty()) {
            return cached.stream().map(json -> {
                try {
                    return objectMapper.readValue(json, SysDictData.class);
                } catch (Exception e) {
                    log.warn("字典数据反序列化失败: {}", e.getMessage());
                    return null;
                }
            }).filter(d -> d != null).collect(Collectors.toList());
        }
        // 从数据库加载并写入缓存
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
        String cacheKey = CACHE_PREFIX + dictType;
        stringRedisTemplate.delete(cacheKey);
        if (list.isEmpty()) return;
        try {
            List<String> jsons = list.stream().map(d -> {
                try {
                    return objectMapper.writeValueAsString(d);
                } catch (Exception e) {
                    log.warn("字典数据序列化失败: id={}, {}", d.getId(), e.getMessage());
                    return null;
                }
            }).filter(j -> j != null).collect(Collectors.toList());
            if (!jsons.isEmpty()) {
                stringRedisTemplate.opsForList().rightPushAll(cacheKey, jsons);
                stringRedisTemplate.expire(cacheKey, CACHE_TTL, TimeUnit.HOURS);
            }
        } catch (Exception e) {
            log.warn("字典缓存写入失败: dictType={}, {}", dictType, e.getMessage());
        }
    }
}
