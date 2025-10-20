package com.example.webclient.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

/**
 * MCP HTTP Stream 客户端
 * 
 * <p>实现 Model Context Protocol 的 Streamable HTTP 传输方式：
 * <ul>
 *   <li>HTTP chunked transfer encoding</li>
 *   <li>流式 JSON 解析</li>
 *   <li>请求/响应关联</li>
 *   <li>背压控制</li>
 * </ul>
 * 
 * @author AI Agent
 * @since 1.0.0
 */
@Component
public class McpStreamClient {

    private static final Logger log = LoggerFactory.getLogger(McpStreamClient.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public McpStreamClient(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 发送 MCP 请求并接收流式响应
     * 
     * <p>通过 HTTP POST 发送请求，接收 chunked 流式响应
     * 
     * @param endpoint MCP 端点
     * @param method 方法名
     * @param params 参数
     * @return Flux 包装的 MCP 消息流
     */
    public Flux<McpMessage> sendStreamRequest(String endpoint, String method, Object params) {
        String requestId = UUID.randomUUID().toString();
        log.info("Sending MCP stream request - id: {}, method: {}", requestId, method);

        McpMessage request = new McpMessage(requestId, method, params);

        return webClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_NDJSON) // 接受 NDJSON 格式
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(McpMessage.class)
                .doOnSubscribe(subscription -> 
                    log.info("Started receiving MCP stream responses"))
                .doOnNext(message -> 
                    log.debug("Received MCP stream message: {}", message))
                .doOnComplete(() -> 
                    log.info("MCP stream completed for request: {}", requestId))
                .doOnError(e -> 
                    log.error("Error in MCP stream for request: {}", requestId, e))
                .timeout(Duration.ofMinutes(5))
                .onErrorResume(e -> {
                    log.error("MCP stream failed", e);
                    return Flux.empty();
                });
    }

    /**
     * 发送 MCP 请求并等待最终结果
     * 
     * <p>接收所有流式消息，返回最后一条消息作为最终结果
     * 
     * @param endpoint MCP 端点
     * @param method 方法名
     * @param params 参数
     * @return Mono 包装的最终响应
     */
    public Mono<McpMessage> sendAndGetFinalResult(String endpoint, String method, Object params) {
        log.info("Sending MCP request and waiting for final result - method: {}", method);

        return sendStreamRequest(endpoint, method, params)
                // 获取最后一条消息
                .last()
                .doOnSuccess(result -> 
                    log.info("Received final MCP result for method: {}", method))
                .doOnError(e -> 
                    log.error("Failed to get final result for method: {}", method, e));
    }

    /**
     * 发送 MCP 请求并收集所有中间结果
     * 
     * @param endpoint MCP 端点
     * @param method 方法名
     * @param params 参数
     * @return Mono 包装的消息列表
     */
    public Mono<java.util.List<McpMessage>> sendAndCollectResults(String endpoint, String method, Object params) {
        log.info("Sending MCP request and collecting all results - method: {}", method);

        return sendStreamRequest(endpoint, method, params)
                .collectList()
                .doOnSuccess(results -> 
                    log.info("Collected {} MCP results for method: {}", results.size(), method));
    }

    /**
     * 发送 MCP 请求并处理流式进度
     * 
     * <p>适用于长时间运行的任务，需要实时报告进度
     * 
     * @param endpoint MCP 端点
     * @param method 方法名
     * @param params 参数
     * @param progressHandler 进度处理器
     * @return Mono 包装的最终结果
     */
    public Mono<McpMessage> sendWithProgress(
            String endpoint, 
            String method, 
            Object params,
            java.util.function.Consumer<McpMessage> progressHandler) {
        
        log.info("Sending MCP request with progress tracking - method: {}", method);

        return sendStreamRequest(endpoint, method, params)
                // 处理每条进度消息
                .doOnNext(message -> {
                    if (progressHandler != null) {
                        progressHandler.accept(message);
                    }
                })
                // 返回最后一条消息
                .last()
                .doOnSuccess(result -> 
                    log.info("MCP request completed with progress - method: {}", method));
    }

    /**
     * 批量发送 MCP 流式请求
     * 
     * @param endpoint MCP 端点
     * @param requests 请求列表
     * @return Flux 包装的响应流
     */
    public Flux<McpMessage> sendBatchStreamRequests(String endpoint, java.util.List<McpMessage> requests) {
        log.info("Sending {} MCP stream requests in batch", requests.size());

        return Flux.fromIterable(requests)
                .flatMap(request -> {
                    if (request.getId() == null) {
                        request.setId(UUID.randomUUID().toString());
                    }
                    return sendStreamRequest(endpoint, request.getMethod(), request.getParams())
                            .onErrorResume(e -> {
                                log.warn("Stream request {} failed: {}", request.getId(), e.getMessage());
                                return Flux.empty();
                            });
                }, 3); // 并发度为 3
    }

    /**
     * 发送 MCP 请求并应用背压控制
     * 
     * @param endpoint MCP 端点
     * @param method 方法名
     * @param params 参数
     * @param prefetch 预取数量
     * @return Flux 包装的消息流
     */
    public Flux<McpMessage> sendWithBackpressure(String endpoint, String method, Object params, int prefetch) {
        log.info("Sending MCP request with backpressure - method: {}, prefetch: {}", method, prefetch);

        return sendStreamRequest(endpoint, method, params)
                .limitRate(prefetch)
                .doOnRequest(n -> log.debug("Requested {} items", n));
    }
}
