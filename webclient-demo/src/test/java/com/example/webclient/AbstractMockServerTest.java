package com.example.webclient;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * MockWebServer 测试基类，为所有客户端测试提供可控的 HTTP 模拟服务。
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractMockServerTest {

    protected static final MockWebServer mockServer;

    static {
        try {
            mockServer = new MockWebServer();
            mockServer.start();
        } catch (Exception ex) {
            throw new IllegalStateException("无法启动 MockWebServer", ex);
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("app.webclient.defaults.base-url", () -> mockServer.url("/").toString());
        registry.add("app.webclient.services.user-service.base-url", () -> mockServer.url("/").toString());
        registry.add("app.webclient.services.recommendation-service.base-url", () -> mockServer.url("/").toString());
        registry.add("app.webclient.services.content-service.base-url", () -> mockServer.url("/").toString());
        registry.add("app.webclient.services.notification-service.base-url", () -> mockServer.url("/").toString());
        registry.add("app.webclient.services.mcp-sse-service.base-url", () -> mockServer.url("/").toString());
        registry.add("app.webclient.services.mcp-stream-service.base-url", () -> mockServer.url("/").toString());
    }

    /**
     * 快速添加一个固定响应。
     *
     * @param response 模拟响应
     */
    protected void enqueue(MockResponse response) {
        mockServer.enqueue(response);
    }
}
