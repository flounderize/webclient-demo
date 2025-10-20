package com.example.webclientdemo.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 持久层配置，启用 Mapper 扫描。
 */
@Configuration
@MapperScan("com.example.webclientdemo.mapper")
public class PersistenceConfiguration {
}
