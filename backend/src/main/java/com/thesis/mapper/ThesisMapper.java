package com.thesis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thesis.entity.Thesis;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ThesisMapper extends BaseMapper<Thesis> {

    /**
     * 物理删除所有论文记录（含软删除的），用于强制重建
     * @return 删除的行数
     */
    @Delete("DELETE FROM t_thesis")
    int physicalDeleteAll();
}
