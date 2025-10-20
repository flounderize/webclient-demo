package com.example.webclientdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.webclientdemo.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表 MyBatis-Plus Mapper。
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
}
