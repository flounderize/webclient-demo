package com.example.webclient.springaimcp.client;

import com.example.webclient.mcp.McpMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * MCP Streamable HTTP 传输客户端
 * 
 * <p>使用 HTTP Chunked Transfer Encoding 与 MCP 服务器通信。
 * 这是 MCP 协议的第三种官方传输方式，特别适用于：
 * <ul>
 *   <li>需要双向流式通信的场景</li>
 *   <li>标准 HTTP/REST 架构集成</li>
 *   <li>长时间运行的任务进度报告</li>
 *   <li>大数据流式传输</li>
 * </ul>
 * 
 * <p>与 SSE 的区别：
 * <ul>
 *   <li>SSE 是单向的（服务器到客户端），Streamable HTTP 可以是双向的</li>
 *   <li>Streamable HTTP 使用 HTTP chunked encoding</li>
 *   <li>每个请求独立建立连接，无需维护长连接</li>
 *   <li>更适合请求-响应模式的流式场景</li>
 * </ul>
 * 
 * @author AI Agent
 * @since 1.0.0
 */
@Component
public class McpStreamableHttpClient {

    private static final Logger log = LoggerFactory.getLogger(McpStreamableHttpClient.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public McpStreamableHttpClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8080").build();
        this.objectMapper = objectMapper;
    }

    /**
     * 发送 MCP 请求并接收流式响应
     * 
     * <p>使用 HTTP POST + chunked encoding，每个 chunk 包含一个 JSON-RPC 消息
     * 
     * @param endpoint MCP 端点
     * @param method 方法名
     * @param params 参数
     * @return Flux 包装的 MCP 消息流
     */
    public Flux<McpMessage> sendStreamRequest(String endpoint, String method, Object params) {
        String requestId = UUID.randomUUID().toString();
        log.info("Sending MCP streamable HTTP request - id: {}, method: {}", requestId, method);

        McpMessage request = new McpMessage(requestId, method, params);

        return webClient.post()
            .uri(endpoint)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_NDJSON) // NDJSON 表示流式 JSON
            .bodyValue(request)
            .retrieve()
            .bodyToFlux(McpMessage.class)
            .doOnSubscribe(sub -> log.info("Started receiving MCP stream responses - request: {}", requestId))
            .doOnNext(message -> log.debug("Received MCP chunk - id: {}, data: {}", requestId, message))
            .doOnComplete(() -> log.info("MCP stream completed - request: {}", requestId))
            .doOnError(e -> log.error("Error in MCP stream - request: {}", requestId, e))
            .timeout(Duration.ofMinutes(5));
    }

    /**
     * 发送请求并等待最终结果
     * 
     * <p>适用于只关心最终结果的场景，忽略中间进度
     * 
     * @param endpoint MCP 端点
     * @param method 方法名
     * @param params 参数
     * @return Mono 包装的最终响应
     */
    public Mono<McpMessage> sendAndGetFinalResult(String endpoint, String method, Object params) {
        log.info("Sending MCP request and waiting for final result - method: {}", method);

        return sendStreamRequest(endpoint, method, params)
            .filter(msg -> msg.getResult() != null || msg.getError() != null)
            .last()
            .doOnSuccess(result -> {
                if (result.getError() != null) {
                    log.error("MCP request failed - error: {}", result.getError());
                } else {
                    log.info("Received final MCP result for method: {}", method);
                }
            });
    }

    /**
     * 发送请求并处理流式进度
     * 
     * <p>适用于长时间运行的任务，需要实时显示进度
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
            .doOnNext(message -> {
                if (progressHandler != null) {
                    progressHandler.accept(message);
                }
            })
            .filter(msg -> msg.getResult() != null || msg.getError() != null)
            .last()
            .doOnSuccess(result -> log.info("MCP request completed with progress - method: {}", method));
    }

    /**
     * 调用工具（流式模式）
     * 
     * <p>某些工具可能返回流式结果，比如文件搜索、日志分析等
     * 
     * @param endpoint MCP 端点
     * @param toolName 工具名称
     * @param arguments 工具参数
     * @return Flux 包装的工具调用结果流
     */
    public Flux<Object> callToolStreaming(String endpoint, String toolName, Map<String, Object> arguments) {
        log.info("Calling tool (streaming): {} with arguments: {}", toolName, arguments);

        Map<String, Object> params = Map.of(
            "name", toolName,
            "arguments", arguments
        );

        return sendStreamRequest(endpoint, "tools/call", params)
            .map(response -> {
                if (response.getError() != null) {
                    throw new RuntimeException("Tool call failed: " + response.getError().getMessage());
                }
                return response.getResult();
            });
    }

    /**
     * 调用工具（等待最终结果）
     * 
     * @param endpoint MCP 端点
     * @param toolName 工具名称
     * @param arguments 工具参数
     * @return Mono 包装的工具调用结果
     */
    public Mono<Object> callTool(String endpoint, String toolName, Map<String, Object> arguments) {
        log.info("Calling tool: {} with arguments: {}", toolName, arguments);

        return callToolStreaming(endpoint, toolName, arguments)
            .last();
    }

    /**
     * 列出可用工具
     * 
     * @param endpoint MCP 端点
     * @return Mono 包装的工具列表
     */
    public Mono<Object> listTools(String endpoint) {
        log.info("Listing available tools");
        return sendAndGetFinalResult(endpoint, "tools/list", null)
            .map(McpMessage::getResult);
    }

    /**
     * 读取资源（流式模式）
     * 
     * <p>适用于大文件或流式资源
     * 
     * @param endpoint MCP 端点
     * @param uri 资源URI
     * @return Flux 包装的资源内容流
     */
    public Flux<Object> readResourceStreaming(String endpoint, String uri) {
        log.info("Reading resource (streaming): {}", uri);

        Map<String, Object> params = Map.of("uri", uri);

        return sendStreamRequest(endpoint, "resources/read", params)
            .map(response -> {
                if (response.getError() != null) {
                    throw new RuntimeException("Resource read failed: " + response.getError().getMessage());
                }
                return response.getResult();
            });
    }

    /**
     * 读取资源（等待完整结果）
     * 
     * @param endpoint MCP 端点
     * @param uri 资源URI
     * @return Mono 包装的资源内容
     */
    public Mono<Object> readResource(String endpoint, String uri) {
        log.info("Reading resource: {}", uri);
        return readResourceStreaming(endpoint, uri).last();
    }

    /**
     * 列出可用资源
     * 
     * @param endpoint MCP 端点
     * @return Mono 包装的资源列表
     */
    public Mono<Object> listResources(String endpoint) {
        log.info("Listing available resources");
        return sendAndGetFinalResult(endpoint, "resources/list", null)
            .map(McpMessage::getResult);
    }

    /**
     * 列出可用提示词
     * 
     * @param endpoint MCP 端点
     * @return Mono 包装的提示词列表
     */
    public Mono<Object> listPrompts(String endpoint) {
        log.info("Listing available prompts");
        return sendAndGetFinalResult(endpoint, "prompts/list", null)
            .map(McpMessage::getResult);
    }

    /**
     * 批量调用工具
     * 
     * @param endpoint MCP 端点
     * @param toolCalls 工具调用列表
     * @return Flux 包装的所有结果
     */
    public Flux<Object> callToolsBatch(String endpoint, java.util.List<Map<String, Object>> toolCalls) {
        log.info("Calling {} tools in batch", toolCalls.size());

        return Flux.fromIterable(toolCalls)
            .flatMap(toolCall -> {
                String toolName = (String) toolCall.get("name");
                @SuppressWarnings("unchecked")
                Map<String, Object> arguments = (Map<String, Object>) toolCall.get("arguments");
                return callTool(endpoint, toolName, arguments)
                    .onErrorResume(e -> {
                        log.warn("Tool call failed for {}: {}", toolName, e.getMessage());
                        return Mono.empty();
                    });
            }, 3); // 并发度为 3
    }

    /**
     * 发送请求并收集所有中间结果
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
     * 发送请求并应用背压控制
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
