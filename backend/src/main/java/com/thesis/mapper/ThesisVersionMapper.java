package com.thesis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thesis.entity.ThesisVersion;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ThesisVersionMapper extends BaseMapper<ThesisVersion> {

    /**
     * 物理删除所有软删除的版本记录（清除唯一约束中的僵尸数据）
     * @return 删除的行数
     */
    @Delete("DELETE FROM t_thesis_version WHERE deleted = 1")
    int physicalDeleteSoftDeleted();

    /**
     * 物理删除所有版本记录（含软删除的），用于强制重建
     * @return 删除的行数
     */
    @Delete("DELETE FROM t_thesis_version")
    int physicalDeleteAll();
}
