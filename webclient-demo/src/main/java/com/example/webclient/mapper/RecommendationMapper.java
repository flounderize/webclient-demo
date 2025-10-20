package com.example.webclient.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.webclient.entity.Recommendation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 推荐表 MyBatis-Plus Mapper。
 */
@Mapper
public interface RecommendationMapper extends BaseMapper<Recommendation> {
}
