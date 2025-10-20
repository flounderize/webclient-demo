package com.example.webclient.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;

/**
 * 测试环境的 WebClient 配置
 * 
 * <p>在测试环境中，使用随机端口启动服务器，需要动态设置 base-url
 * 
 * @author AI Agent
 * @since 1.0.0
 */
@TestConfiguration
public class TestWebClientConfig {

    @LocalServerPort
    private int port;

    /**
     * 设置测试环境的 base-url 属性
     * 
     * @return 包含正确端口的 base-url
     */
    @Bean
    public String testBaseUrl() {
        System.setProperty("webclient.base-url", "http://localhost:" + port);
        return "http://localhost:" + port;
    }
}
