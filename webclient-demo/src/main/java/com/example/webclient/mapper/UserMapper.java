package com.example.webclient.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.webclient.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表 MyBatis-Plus Mapper。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
