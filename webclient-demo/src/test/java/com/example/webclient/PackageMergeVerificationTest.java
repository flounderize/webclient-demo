package com.example.webclient;

import com.example.webclient.client.UserProfileClient;
import com.example.webclient.config.WebClientConfiguration;
import com.example.webclient.support.WebClientAuthFilter;
import com.example.webclient.support.WebClientErrorHandler;
import com.example.webclient.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 包合并验证测试
 * 
 * <p>验证从 webclientdemo 合并到 webclient 包的组件是否正确加载
 */
@SpringBootTest
@ActiveProfiles("test")
class PackageMergeVerificationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void testCoreBeansAreLoaded() {
        // 验证核心配置类
        assertNotNull(applicationContext.getBean(WebClientConfiguration.class), 
            "WebClientConfiguration should be loaded");
        assertNotNull(applicationContext.getBean(WebClientConfiguration.WebClientRegistry.class), 
            "WebClientRegistry should be loaded");
        
        // 验证支持类
        assertNotNull(applicationContext.getBean(WebClientAuthFilter.class), 
            "WebClientAuthFilter should be loaded");
        assertNotNull(applicationContext.getBean(WebClientErrorHandler.class), 
            "WebClientErrorHandler should be loaded");
    }

    @Test
    void testClientBeansAreLoaded() {
        // 验证客户端类
        assertNotNull(applicationContext.getBean(UserProfileClient.class), 
            "UserProfileClient should be loaded");
        // 可以添加更多客户端验证
    }

    @Test
    void testMapperBeansAreLoaded() {
        // 验证 MyBatis Mapper
        assertNotNull(applicationContext.getBean(UserMapper.class), 
            "UserMapper should be loaded");
    }

    @Test
    void testNoWebclientdemoPackageComponents() {
        // 验证不存在 webclientdemo 包的组件
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            String packageName = bean.getClass().getPackage() != null ? 
                bean.getClass().getPackage().getName() : "";
            assertFalse(packageName.contains("webclientdemo"), 
                "No beans should be from webclientdemo package, found: " + beanName + " in " + packageName);
        }
    }
}
