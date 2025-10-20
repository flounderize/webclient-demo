package com.example.webclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * WebClient 示例工程主应用类
 * 
 * <p>本应用演示了 Spring WebClient 的各种使用场景：
 * <ul>
 *   <li>同步调用 (Mono.block)</li>
 *   <li>异步调用 (Mono/Flux)</li>
 *   <li>响应式流式调用 (Flux stream)</li>
 *   <li>SSE (Server-Sent Events) 调用</li>
 *   <li>MCP (Model Context Protocol) 调用</li>
 * </ul>
 * 
 * @author AI Agent
 * @since 1.0.0
 */
@SpringBootApplication
public class WebClientDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebClientDemoApplication.class, args);
    }
}
