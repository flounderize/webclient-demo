package com.example.webclient.springaimcp.client;

import com.example.webclient.mcp.McpMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP SSE 传输客户端（基于 spring-ai-mcp 规范）
 * 
 * <p>使用 Server-Sent Events (SSE) 与 MCP 服务器通信。
 * 这是 MCP 协议的官方传输方式之一，特别适用于：
 * <ul>
 *   <li>远程 HTTP 服务器通信</li>
 *   <li>单向服务器推送场景</li>
 *   <li>需要自动重连的场景</li>
 * </ul>
 * 
 * <p>与纯 WebClient SSE 的区别：
 * <ul>
 *   <li>严格遵循 MCP 协议规范</li>
 *   <li>支持 MCP 的初始化握手流程</li>
 *   <li>实现 JSON-RPC 2.0 消息格式</li>
 *   <li>支持 MCP 特定的事件类型（endpoint、message、error）</li>
 * </ul>
 * 
 * @author AI Agent
 * @since 1.0.0
 */
@Component
public class McpSseTransportClient {

    private static final Logger log = LoggerFactory.getLogger(McpSseTransportClient.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final Map<String, Sinks.One<McpMessage>> pendingRequests = new ConcurrentHashMap<>();
    
    private Flux<ServerSentEvent<String>> sseConnection;
    private String sessionId;

    public McpSseTransportClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8080").build();
        this.objectMapper = objectMapper;
    }

    /**
     * 建立 MCP SSE 连接并完成握手
     * 
     * <p>MCP SSE 握手流程：
     * 1. 客户端发起 SSE 连接
     * 2. 服务器返回 endpoint 事件，包含可用的 HTTP endpoint
     * 3. 客户端通过该 endpoint 发送请求
     * 4. 服务器通过 SSE 推送响应
     * 
     * @param endpoint SSE 端点
     * @return Mono 包装的会话信息
     */
    public Mono<Map<String, Object>> connect(String endpoint) {
        log.info("Connecting to MCP SSE endpoint: {}", endpoint);
        
        return Mono.fromRunnable(() -> {
            this.sessionId = UUID.randomUUID().toString();
            
            // 建立 SSE 连接
            this.sseConnection = webClient.get()
                .uri(endpoint)
                .header("X-Session-Id", sessionId)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(ServerSentEvent.class)
                .map(event -> (ServerSentEvent<String>) event)
                .doOnSubscribe(sub -> log.info("MCP SSE connection established - session: {}", sessionId))
                .doOnNext(event -> log.debug("Received SSE event - type: {}, data: {}", event.event(), event.data()))
                .doOnError(e -> log.error("Error in MCP SSE connection", e))
                .retryWhen(Retry.backoff(5, Duration.ofSeconds(2))
                    .maxBackoff(Duration.ofSeconds(30))
                    .doBeforeRetry(signal -> log.info("Retrying MCP SSE connection, attempt: {}", signal.totalRetries() + 1)))
                .share(); // 共享连接
            
            // 启动消息处理
            startMessageProcessor();
        })
        .then(waitForEndpoint())
        .doOnSuccess(info -> log.info("MCP SSE handshake completed: {}", info));
    }

    /**
     * 等待服务器发送 endpoint 事件（握手流程）
     */
    @SuppressWarnings("unchecked")
    private Mono<Map<String, Object>> waitForEndpoint() {
        return sseConnection
            .filter(event -> "endpoint".equals(event.event()))
            .next()
            .map(event -> {
                try {
                    String data = event.data();
                    return (Map<String, Object>) objectMapper.readValue(data, Map.class);
                } catch (Exception e) {
                    log.error("Failed to parse endpoint event", e);
                    throw new RuntimeException("Failed to parse endpoint event", e);
                }
            })
            .timeout(Duration.ofSeconds(10));
    }

    /**
     * 启动消息处理器
     */
    private void startMessageProcessor() {
        sseConnection
            .filter(event -> "message".equals(event.event()))
            .subscribe(event -> {
                try {
                    String data = event.data();
                    McpMessage message = objectMapper.readValue(data, McpMessage.class);
                    handleMessage(message);
                } catch (Exception e) {
                    log.error("Failed to process message event", e);
                }
            });
    }

    /**
     * 发送 MCP 请求
     * 
     * <p>注意：在 SSE 传输模式下，请求通过 HTTP POST 发送，响应通过 SSE 接收
     * 
     * @param requestEndpoint 请求端点（从握手中获得）
     * @param method 方法名
     * @param params 参数
     * @return Mono 包装的响应消息
     */
    public Mono<McpMessage> sendRequest(String requestEndpoint, String method, Object params) {
        String requestId = UUID.randomUUID().toString();
        log.info("Sending MCP SSE request - id: {}, method: {}", requestId, method);

        McpMessage request = new McpMessage(requestId, method, params);
        
        // 创建响应接收器
        Sinks.One<McpMessage> responseSink = Sinks.one();
        pendingRequests.put(requestId, responseSink);

        return webClient.post()
            .uri(requestEndpoint)
            .header("X-Session-Id", sessionId)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(String.class) // 服务器可能返回 ACK
            .then(responseSink.asMono())
            .timeout(Duration.ofSeconds(30))
            .doOnError(e -> {
                log.error("MCP SSE request failed - id: {}", requestId, e);
                pendingRequests.remove(requestId);
            });
    }

    /**
     * 调用工具
     * 
     * @param requestEndpoint 请求端点
     * @param toolName 工具名称
     * @param arguments 工具参数
     * @return Mono 包装的工具调用结果
     */
    public Mono<Object> callTool(String requestEndpoint, String toolName, Map<String, Object> arguments) {
        log.info("Calling tool: {} with arguments: {}", toolName, arguments);
        
        Map<String, Object> params = Map.of(
            "name", toolName,
            "arguments", arguments
        );
        
        return sendRequest(requestEndpoint, "tools/call", params)
            .map(response -> {
                if (response.getError() != null) {
                    throw new RuntimeException("Tool call failed: " + response.getError().getMessage());
                }
                return response.getResult();
            });
    }

    /**
     * 列出可用工具
     * 
     * @param requestEndpoint 请求端点
     * @return Mono 包装的工具列表
     */
    public Mono<Object> listTools(String requestEndpoint) {
        log.info("Listing available tools");
        return sendRequest(requestEndpoint, "tools/list", null)
            .map(McpMessage::getResult);
    }

    /**
     * 读取资源
     * 
     * @param requestEndpoint 请求端点
     * @param uri 资源URI
     * @return Mono 包装的资源内容
     */
    public Mono<Object> readResource(String requestEndpoint, String uri) {
        log.info("Reading resource: {}", uri);
        
        Map<String, Object> params = Map.of("uri", uri);
        
        return sendRequest(requestEndpoint, "resources/read", params)
            .map(response -> {
                if (response.getError() != null) {
                    throw new RuntimeException("Resource read failed: " + response.getError().getMessage());
                }
                return response.getResult();
            });
    }

    /**
     * 列出可用资源
     * 
     * @param requestEndpoint 请求端点
     * @return Mono 包装的资源列表
     */
    public Mono<Object> listResources(String requestEndpoint) {
        log.info("Listing available resources");
        return sendRequest(requestEndpoint, "resources/list", null)
            .map(McpMessage::getResult);
    }

    /**
     * 订阅服务器通知
     * 
     * @return Flux 包装的通知消息流
     */
    public Flux<McpMessage> subscribeNotifications() {
        log.info("Subscribing to server notifications");
        
        return sseConnection
            .filter(event -> "notification".equals(event.event()))
            .mapNotNull(event -> {
                try {
                    String data = event.data();
                    return objectMapper.readValue(data, McpMessage.class);
                } catch (Exception e) {
                    log.error("Failed to parse notification", e);
                    return null;
                }
            })
            .doOnNext(notification -> log.info("Received notification: {}", notification.getMethod()));
    }

    /**
     * 处理接收到的消息
     */
    private void handleMessage(McpMessage message) {
        String messageId = message.getId();
        
        if (messageId != null && pendingRequests.containsKey(messageId)) {
            // 这是对某个请求的响应
            Sinks.One<McpMessage> sink = pendingRequests.remove(messageId);
            sink.tryEmitValue(message);
        }
    }

    /**
     * 关闭连接
     * 
     * @return Mono 表示关闭完成
     */
    public Mono<Void> disconnect() {
        return Mono.fromRunnable(() -> {
            log.info("Closing MCP SSE connection - session: {}", sessionId);
            pendingRequests.clear();
            sessionId = null;
        });
    }
}
