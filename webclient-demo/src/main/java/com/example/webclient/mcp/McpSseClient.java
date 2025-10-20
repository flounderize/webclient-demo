package com.example.webclient.mcp;

import com.example.webclient.exception.WebClientException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.UUID;

/**
 * MCP SSE 客户端
 * 
 * <p>实现 Model Context Protocol 的 SSE 传输方式：
 * <ul>
 *   <li>SSE handshake</li>
 *   <li>消息序列化/反序列化</li>
 *   <li>请求/响应关联</li>
 *   <li>错误处理和重连</li>
 * </ul>
 * 
 * @author AI Agent
 * @since 1.0.0
 */
@Component
public class McpSseClient {

    private static final Logger log = LoggerFactory.getLogger(McpSseClient.class);

    private final WebClient sseWebClient;
    private final ObjectMapper objectMapper;

    public McpSseClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.sseWebClient = webClientBuilder.baseUrl("http://localhost:8080").build();
        this.objectMapper = objectMapper;
    }

    /**
     * 建立 MCP SSE 连接
     * 
     * <p>通过 SSE 建立 MCP 连接，接收服务端推送的消息
     * 
     * @param endpoint MCP 端点
     * @return Flux 包装的 MCP 消息流
     */
    public Flux<McpMessage> connect(String endpoint) {
        log.info("Connecting to MCP SSE endpoint: {}", endpoint);

        return sseWebClient.get()
                .uri(endpoint)
                .retrieve()
                .bodyToFlux(ServerSentEvent.class)
                .mapNotNull(event -> {
                    try {
                        String eventType = event.event();
                        Object data = event.data();
                        
                        log.debug("Received MCP SSE event - type: {}, data: {}", eventType, data);
                        
                        if (data == null) {
                            return null;
                        }

                        // 将数据转换为 McpMessage
                        if (data instanceof String) {
                            return objectMapper.readValue((String) data, McpMessage.class);
                        } else {
                            return objectMapper.convertValue(data, McpMessage.class);
                        }
                    } catch (Exception e) {
                        log.error("Failed to parse MCP message", e);
                        return null;
                    }
                })
                .doOnSubscribe(subscription -> log.info("MCP SSE connection established"))
                .doOnNext(message -> log.debug("Received MCP message: {}", message))
                .doOnComplete(() -> log.info("MCP SSE stream completed"))
                .doOnError(e -> log.error("Error in MCP SSE stream", e))
                // 自动重连
                .retryWhen(Retry.backoff(5, Duration.ofSeconds(2))
                        .maxBackoff(Duration.ofSeconds(30))
                        .doBeforeRetry(retrySignal -> 
                            log.info("Retrying MCP SSE connection, attempt: {}", retrySignal.totalRetries() + 1)));
    }

    /**
     * 发送 MCP 请求并等待响应
     * 
     * <p>注意：SSE 是单向通信，这里模拟的是在另一个连接发送请求，在 SSE 连接接收响应
     * 
     * @param endpoint MCP 端点
     * @param method 方法名
     * @param params 参数
     * @return Mono 包装的响应消息
     */
    public Mono<McpMessage> sendRequest(String endpoint, String method, Object params) {
        String requestId = UUID.randomUUID().toString();
        log.info("Sending MCP request - id: {}, method: {}", requestId, method);

        McpMessage request = new McpMessage(requestId, method, params);

        // 建立 SSE 连接并过滤出对应的响应
        return connect(endpoint)
                // 只保留 id 匹配的响应
                .filter(message -> requestId.equals(message.getId()))
                // 只取第一个响应
                .next()
                // 设置超时
                .timeout(Duration.ofSeconds(30))
                .doOnSuccess(response -> {
                    if (response != null && response.getError() != null) {
                        log.error("MCP request failed - error: {}", response.getError());
                    } else {
                        log.info("MCP request succeeded - id: {}", requestId);
                    }
                })
                .doOnError(e -> log.error("MCP request timeout or error - id: {}", requestId, e));
    }

    /**
     * 订阅 MCP 通知
     * 
     * <p>只接收通知类型的消息（没有 id 的消息）
     * 
     * @param endpoint MCP 端点
     * @return Flux 包装的通知消息流
     */
    public Flux<McpMessage> subscribeNotifications(String endpoint) {
        log.info("Subscribing to MCP notifications: {}", endpoint);

        return connect(endpoint)
                // 过滤出通知消息（没有 id 的消息）
                .filter(message -> message.getId() == null && message.getMethod() != null)
                .doOnNext(notification -> 
                    log.info("Received MCP notification - method: {}", notification.getMethod()));
    }

    /**
     * 订阅特定方法的通知
     * 
     * @param endpoint MCP 端点
     * @param method 方法名
     * @return Flux 包装的通知消息流
     */
    public Flux<McpMessage> subscribeNotificationsByMethod(String endpoint, String method) {
        log.info("Subscribing to MCP notifications for method: {}", method);

        return subscribeNotifications(endpoint)
                .filter(message -> method.equals(message.getMethod()))
                .doOnNext(notification -> 
                    log.info("Received {} notification", method));
    }

    /**
     * 批量发送请求并收集响应
     * 
     * @param endpoint MCP 端点
     * @param requests 请求列表
     * @return Flux 包装的响应流
     */
    public Flux<McpMessage> sendBatchRequests(String endpoint, java.util.List<McpMessage> requests) {
        log.info("Sending {} MCP requests in batch", requests.size());

        return Flux.fromIterable(requests)
                .flatMap(request -> {
                    if (request.getId() == null) {
                        request.setId(UUID.randomUUID().toString());
                    }
                    return sendRequest(endpoint, request.getMethod(), request.getParams())
                            .onErrorResume(e -> {
                                log.warn("Request {} failed: {}", request.getId(), e.getMessage());
                                return Mono.empty();
                            });
                }, 3); // 并发度为 3
    }
}
