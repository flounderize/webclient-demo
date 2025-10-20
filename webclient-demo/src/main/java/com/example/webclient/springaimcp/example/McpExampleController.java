package com.example.webclient.springaimcp.example;

import com.example.webclient.mcp.McpMessage;
import com.example.webclient.springaimcp.client.McpSseTransportClient;
import com.example.webclient.springaimcp.client.McpStdioClient;
import com.example.webclient.springaimcp.client.McpStreamableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * MCP 示例控制器
 * 
 * <p>演示如何使用不同的 MCP 传输方式：
 * <ul>
 *   <li>STDIO - 标准输入输出传输</li>
 *   <li>SSE - Server-Sent Events 传输</li>
 *   <li>Streamable HTTP - HTTP 流式传输</li>
 * </ul>
 * 
 * <p>包含完整的工具调用、资源读取、提示词管理示例
 * 
 * @author AI Agent
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/examples/mcp")
public class McpExampleController {

    private static final Logger log = LoggerFactory.getLogger(McpExampleController.class);

    private final McpStdioClient stdioClient;
    private final McpSseTransportClient sseClient;
    private final McpStreamableHttpClient streamClient;

    public McpExampleController(
            McpStdioClient stdioClient,
            McpSseTransportClient sseClient,
            McpStreamableHttpClient streamClient) {
        this.stdioClient = stdioClient;
        this.sseClient = sseClient;
        this.streamClient = streamClient;
    }

    // ========== STDIO 传输示例 ==========

    /**
     * STDIO 工具调用示例
     */
    @PostMapping("/stdio/tool/call")
    public Mono<Object> stdioToolCallExample(@RequestBody Map<String, Object> request) {
        String toolName = (String) request.get("tool");
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) request.get("arguments");
        
        log.info("STDIO example - calling tool: {} with arguments: {}", toolName, arguments);
        
        return stdioClient.callTool(toolName, arguments)
            .doOnSuccess(result -> log.info("STDIO tool call result: {}", result));
    }

    /**
     * STDIO 列出工具示例
     */
    @GetMapping("/stdio/tools")
    public Mono<Object> stdioListToolsExample() {
        log.info("STDIO example - listing tools");
        return stdioClient.listTools();
    }

    /**
     * STDIO 资源读取示例
     */
    @GetMapping("/stdio/resource")
    public Mono<Object> stdioReadResourceExample(@RequestParam String uri) {
        log.info("STDIO example - reading resource: {}", uri);
        return stdioClient.readResource(uri);
    }

    /**
     * STDIO 提示词获取示例
     */
    @PostMapping("/stdio/prompt")
    public Mono<Object> stdioGetPromptExample(@RequestBody Map<String, Object> request) {
        String promptName = (String) request.get("name");
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) request.get("arguments");
        
        log.info("STDIO example - getting prompt: {} with arguments: {}", promptName, arguments);
        return stdioClient.getPrompt(promptName, arguments);
    }

    // ========== SSE 传输示例 ==========

    /**
     * SSE 连接示例
     */
    @PostMapping("/sse/connect")
    public Mono<Map<String, Object>> sseConnectExample() {
        log.info("SSE example - connecting to MCP server");
        return sseClient.connect("/api/springai/mcp/sse")
            .doOnSuccess(info -> log.info("SSE connection established: {}", info));
    }

    /**
     * SSE 工具调用示例
     */
    @PostMapping("/sse/tool/call")
    public Mono<Object> sseToolCallExample(@RequestBody Map<String, Object> request) {
        String endpoint = (String) request.get("endpoint");
        String toolName = (String) request.get("tool");
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) request.get("arguments");
        
        log.info("SSE example - calling tool: {} with arguments: {}", toolName, arguments);
        
        return sseClient.callTool(endpoint, toolName, arguments)
            .doOnSuccess(result -> log.info("SSE tool call result: {}", result));
    }

    /**
     * SSE 订阅通知示例
     */
    @GetMapping("/sse/notifications")
    public Flux<McpMessage> sseNotificationsExample() {
        log.info("SSE example - subscribing to notifications");
        return sseClient.subscribeNotifications()
            .doOnNext(notification -> 
                log.info("Received notification: {}", notification.getMethod()));
    }

    /**
     * SSE 资源列表示例
     */
    @GetMapping("/sse/resources")
    public Mono<Object> sseListResourcesExample(@RequestParam String endpoint) {
        log.info("SSE example - listing resources");
        return sseClient.listResources(endpoint);
    }

    // ========== Streamable HTTP 传输示例 ==========

    /**
     * HTTP Stream 工具调用示例（流式）
     */
    @PostMapping("/stream/tool/call")
    public Flux<Object> streamToolCallExample(@RequestBody Map<String, Object> request) {
        String endpoint = (String) request.getOrDefault("endpoint", "/api/springai/mcp/stream");
        String toolName = (String) request.get("tool");
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) request.get("arguments");
        
        log.info("Stream example - calling tool (streaming): {} with arguments: {}", toolName, arguments);
        
        return streamClient.callToolStreaming(endpoint, toolName, arguments)
            .doOnNext(result -> log.info("Stream chunk received: {}", result));
    }

    /**
     * HTTP Stream 工具调用示例（带进度）
     */
    @PostMapping("/stream/tool/call-with-progress")
    public Mono<McpMessage> streamToolCallWithProgressExample(@RequestBody Map<String, Object> request) {
        String endpoint = (String) request.getOrDefault("endpoint", "/api/springai/mcp/stream");
        String toolName = (String) request.get("tool");
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) request.get("arguments");
        
        log.info("Stream example - calling tool with progress: {}", toolName);
        
        return streamClient.sendWithProgress(
            endpoint,
            "tools/call",
            Map.of("name", toolName, "arguments", arguments),
            progress -> log.info("Progress update: {}", progress.getResult())
        );
    }

    /**
     * HTTP Stream 资源读取示例（流式）
     */
    @GetMapping("/stream/resource")
    public Flux<Object> streamReadResourceExample(@RequestParam String uri) {
        log.info("Stream example - reading resource (streaming): {}", uri);
        return streamClient.readResourceStreaming("/api/springai/mcp/stream", uri)
            .doOnNext(chunk -> log.info("Resource chunk: {}", chunk));
    }

    /**
     * HTTP Stream 长任务示例
     */
    @PostMapping("/stream/long-task")
    public Flux<McpMessage> streamLongTaskExample(@RequestParam(defaultValue = "5") int steps) {
        log.info("Stream example - executing long task with {} steps", steps);
        
        return streamClient.sendStreamRequest(
            "/api/springai/mcp/stream",
            "long_task",
            Map.of("steps", steps)
        ).doOnNext(message -> {
            if ("progress".equals(message.getMethod())) {
                log.info("Long task progress: {}", message.getResult());
            } else {
                log.info("Long task completed: {}", message.getResult());
            }
        });
    }

    /**
     * HTTP Stream 批量工具调用示例
     */
    @PostMapping("/stream/tools/batch")
    public Flux<Object> streamBatchToolsExample(@RequestBody List<Map<String, Object>> toolCalls) {
        log.info("Stream example - calling {} tools in batch", toolCalls.size());
        
        return streamClient.callToolsBatch("/api/springai/mcp/stream", toolCalls)
            .doOnNext(result -> log.info("Batch result: {}", result));
    }

    // ========== 综合示例 ==========

    /**
     * 综合示例：使用不同传输方式调用相同的工具
     */
    @PostMapping("/compare/tool-call")
    public Mono<Map<String, Object>> compareTransportsExample(@RequestBody Map<String, Object> request) {
        String toolName = (String) request.get("tool");
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) request.get("arguments");
        
        log.info("Comparing transports - calling tool: {}", toolName);
        
        // 使用 Stream 传输（最简单的 HTTP 方式）
        Mono<Object> streamResult = streamClient.callTool("/api/springai/mcp/stream", toolName, arguments)
            .doOnSuccess(r -> log.info("Stream transport result: {}", r))
            .onErrorResume(e -> {
                log.error("Stream transport failed", e);
                return Mono.just(Map.of("error", e.getMessage()));
            });
        
        return streamResult.map(result -> Map.of(
            "tool", toolName,
            "arguments", arguments,
            "streamResult", result,
            "note", "STDIO 和 SSE 传输需要额外的连接设置"
        ));
    }

    /**
     * 综合示例：多传输方式并发调用
     */
    @PostMapping("/concurrent/tool-call")
    public Mono<Map<String, Object>> concurrentTransportsExample(@RequestBody Map<String, Object> request) {
        String toolName = (String) request.get("tool");
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) request.get("arguments");
        
        log.info("Concurrent example - calling tool on multiple transports: {}", toolName);
        
        // 只使用 Stream 传输演示（其他需要预先连接）
        return streamClient.callTool("/api/springai/mcp/stream", toolName, arguments)
            .map(result -> Map.of(
                "tool", toolName,
                "transports", Map.of(
                    "streamableHttp", result
                ),
                "note", "STDIO 和 SSE 需要预先建立连接"
            ))
            .onErrorResume(e -> Mono.just(Map.of(
                "error", e.getMessage(),
                "tool", toolName
            )));
    }

    /**
     * 提示词管理综合示例
     */
    @GetMapping("/prompts/comprehensive")
    public Mono<Map<String, Object>> promptsComprehensiveExample() {
        log.info("Comprehensive prompts example");
        
        // 列出可用提示词
        Mono<Object> listPrompts = streamClient.listPrompts("/api/springai/mcp/stream")
            .doOnSuccess(prompts -> log.info("Available prompts: {}", prompts));
        
        return listPrompts.map(prompts -> Map.of(
            "availablePrompts", prompts,
            "note", "使用 POST /api/examples/mcp/stream/prompt 获取具体提示词"
        ));
    }

    /**
     * 获取提示词示例
     */
    @PostMapping("/stream/prompt")
    public Mono<McpMessage> getPromptExample(@RequestBody Map<String, Object> request) {
        String promptName = (String) request.get("name");
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) request.getOrDefault("arguments", Map.of());
        
        log.info("Getting prompt: {} with arguments: {}", promptName, arguments);
        
        return streamClient.sendAndGetFinalResult(
            "/api/springai/mcp/stream",
            "prompts/get",
            Map.of("name", promptName, "arguments", arguments)
        );
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Mono<Map<String, Object>> health() {
        return Mono.just(Map.of(
            "status", "ok",
            "availableTransports", List.of("stdio", "sse", "streamableHttp"),
            "endpoints", Map.of(
                "stdio", "/api/examples/mcp/stdio/*",
                "sse", "/api/examples/mcp/sse/*",
                "stream", "/api/examples/mcp/stream/*"
            )
        ));
    }
}
