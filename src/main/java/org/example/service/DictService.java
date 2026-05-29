package org.example.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.entity.SysDictData;
import org.example.entity.SysDictType;

import java.util.List;

public interface DictService {

    // ====== 字典类型 ======
    Page<SysDictType> getTypesByPage(int page, int size);
    List<SysDictType> getAllTypes();
    SysDictType getTypeById(Long id);
    void saveType(SysDictType dictType);
    void updateType(SysDictType dictType);
    void deleteType(Long id);

    // ====== 字典数据 ======
    Page<SysDictData> getDataByPage(int page, int size, String dictType);
    List<SysDictData> getDataByType(String dictType);
    SysDictData getDataById(Long id);
    void saveData(SysDictData dictData);
    void updateData(SysDictData dictData);
    void deleteData(Long id);

    /** 刷新字典缓存 */
    void refreshCache();
}
