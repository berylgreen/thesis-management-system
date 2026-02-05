package com.thesis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.thesis.entity.Review;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {
}
