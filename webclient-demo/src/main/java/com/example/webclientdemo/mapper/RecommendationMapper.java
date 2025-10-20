package com.example.webclientdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.webclientdemo.entity.RecommendationEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 推荐表 MyBatis-Plus Mapper。
 */
@Mapper
public interface RecommendationMapper extends BaseMapper<RecommendationEntity> {
}
