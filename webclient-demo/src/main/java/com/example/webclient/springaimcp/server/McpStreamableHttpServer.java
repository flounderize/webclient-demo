package com.example.webclient.springaimcp.server;

import com.example.webclient.mcp.McpMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * MCP Streamable HTTP 传输服务器
 * 
 * <p>通过 HTTP Chunked Transfer Encoding 提供 MCP 服务。
 * 这是 MCP 协议的第三种官方服务器实现，特别适用于：
 * <ul>
 *   <li>需要双向流式通信的场景</li>
 *   <li>标准 HTTP/REST 架构</li>
 *   <li>长时间运行任务的进度报告</li>
 *   <li>大数据流式处理</li>
 * </ul>
 * 
 * <p>实现特点：
 * <ul>
 *   <li>使用 HTTP chunked encoding 流式返回响应</li>
 *   <li>每个 chunk 包含一个 JSON-RPC 消息</li>
 *   <li>支持中间进度报告</li>
 *   <li>客户端友好的请求-响应模式</li>
 * </ul>
 * 
 * @author AI Agent
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/springai/mcp/stream")
public class McpStreamableHttpServer {

    private static final Logger log = LoggerFactory.getLogger(McpStreamableHttpServer.class);

    private final ObjectMapper objectMapper;

    public McpStreamableHttpServer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 处理 MCP 流式请求
     * 
     * <p>接收请求并返回流式响应，使用 NDJSON 格式（每行一个 JSON 对象）
     * 
     * @param request MCP 请求
     * @return 流式 MCP 响应
     */
    @PostMapping(produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<McpMessage> handleStreamRequest(@RequestBody McpMessage request) {
        log.info("Received MCP stream request - id: {}, method: {}", 
            request.getId(), request.getMethod());

        String method = request.getMethod();
        Object params = request.getParams();
        String requestId = request.getId();

        return switch (method) {
            case "tools/list" -> Flux.just(createResponse(requestId, listTools()));
            case "tools/call" -> callToolStreaming(requestId, params);
            case "resources/list" -> Flux.just(createResponse(requestId, listResources()));
            case "resources/read" -> readResourceStreaming(requestId, params);
            case "prompts/list" -> Flux.just(createResponse(requestId, listPrompts()));
            case "prompts/get" -> Flux.just(createResponse(requestId, getPrompt(params)));
            case "initialize" -> Flux.just(createResponse(requestId, initialize(params)));
            case "long_task" -> executeLongTask(requestId, params);
            default -> Flux.just(createError(requestId, -32601, "Method not found: " + method));
        };
    }

    /**
     * 创建响应消息
     */
    private McpMessage createResponse(String requestId, Object result) {
        McpMessage response = new McpMessage();
        response.setId(requestId);
        response.setResult(result);
        return response;
    }

    /**
     * 创建错误消息
     */
    private McpMessage createError(String requestId, int code, String message) {
        McpMessage response = new McpMessage();
        response.setId(requestId);
        response.setError(new McpMessage.McpError(code, message));
        return response;
    }

    /**
     * 创建进度消息
     */
    private McpMessage createProgress(String requestId, int progress, String message) {
        McpMessage progressMsg = new McpMessage();
        progressMsg.setId(requestId);
        progressMsg.setMethod("progress");
        progressMsg.setResult(Map.of(
            "progress", progress,
            "message", message
        ));
        return progressMsg;
    }

    // ========== 工具相关方法实现 ==========

    private Object listTools() {
        return Map.of(
            "tools", List.of(
                Map.of(
                    "name", "search",
                    "description", "搜索文件或内容",
                    "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "query", Map.of("type", "string", "description", "搜索关键词")
                        ),
                        "required", List.of("query")
                    )
                ),
                Map.of(
                    "name", "analyze",
                    "description", "分析数据",
                    "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "data", Map.of("type", "string", "description", "要分析的数据")
                        ),
                        "required", List.of("data")
                    )
                )
            )
        );
    }

    @SuppressWarnings("unchecked")
    private Flux<McpMessage> callToolStreaming(String requestId, Object params) {
        Map<String, Object> paramsMap = (Map<String, Object>) params;
        String toolName = (String) paramsMap.get("name");
        Map<String, Object> arguments = (Map<String, Object>) paramsMap.get("arguments");
        
        log.info("Calling tool (streaming): {} with arguments: {}", toolName, arguments);
        
        if ("search".equals(toolName)) {
            String query = (String) arguments.get("query");
            
            // 模拟流式搜索结果
            return Flux.interval(Duration.ofMillis(200))
                .take(5)
                .map(i -> {
                    if (i < 4) {
                        // 中间结果
                        return createProgress(requestId, (int)((i + 1) * 20), 
                            "Found result " + (i + 1) + " for query: " + query);
                    } else {
                        // 最终结果
                        return createResponse(requestId, Map.of(
                            "content", List.of(
                                Map.of("type", "text", 
                                    "text", "Search completed. Found 4 results for: " + query)
                            )
                        ));
                    }
                });
        } else if ("analyze".equals(toolName)) {
            String data = (String) arguments.get("data");
            
            // 模拟流式分析
            return Flux.interval(Duration.ofMillis(300))
                .take(3)
                .map(i -> {
                    if (i < 2) {
                        return createProgress(requestId, (int)((i + 1) * 33), 
                            "Analyzing data... step " + (i + 1));
                    } else {
                        return createResponse(requestId, Map.of(
                            "content", List.of(
                                Map.of("type", "text", 
                                    "text", "Analysis completed for: " + data)
                            )
                        ));
                    }
                });
        }
        
        return Flux.just(createError(requestId, -32602, "Unknown tool: " + toolName));
    }

    // ========== 资源相关方法实现 ==========

    private Object listResources() {
        return Map.of(
            "resources", List.of(
                Map.of(
                    "uri", "file:///data/large_file.txt",
                    "name", "Large Data File",
                    "description", "大文件数据",
                    "mimeType", "text/plain"
                ),
                Map.of(
                    "uri", "file:///data/stream.log",
                    "name", "Streaming Logs",
                    "description", "流式日志",
                    "mimeType", "text/plain"
                )
            )
        );
    }

    @SuppressWarnings("unchecked")
    private Flux<McpMessage> readResourceStreaming(String requestId, Object params) {
        Map<String, Object> paramsMap = (Map<String, Object>) params;
        String uri = (String) paramsMap.get("uri");
        
        log.info("Reading resource (streaming): {}", uri);
        
        // 模拟流式读取大文件
        return Flux.interval(Duration.ofMillis(100))
            .take(10)
            .map(i -> {
                if (i < 9) {
                    // 分块内容
                    return createProgress(requestId, (int)((i + 1) * 10), 
                        "Chunk " + (i + 1) + " of " + uri);
                } else {
                    // 最终结果
                    return createResponse(requestId, Map.of(
                        "contents", List.of(
                            Map.of(
                                "uri", uri,
                                "mimeType", "text/plain",
                                "text", "Complete content of " + uri + " (all chunks merged)"
                            )
                        )
                    ));
                }
            });
    }

    // ========== 提示词相关方法实现 ==========

    private Object listPrompts() {
        return Map.of(
            "prompts", List.of(
                Map.of(
                    "name", "code_generation",
                    "description", "代码生成提示词",
                    "arguments", List.of(
                        Map.of("name", "language", "description", "编程语言", "required", true),
                        Map.of("name", "description", "description", "功能描述", "required", true)
                    )
                )
            )
        );
    }

    @SuppressWarnings("unchecked")
    private Object getPrompt(Object params) {
        Map<String, Object> paramsMap = (Map<String, Object>) params;
        String promptName = (String) paramsMap.get("name");
        Map<String, Object> arguments = (Map<String, Object>) paramsMap.getOrDefault("arguments", Map.of());
        
        log.info("Getting prompt: {} with arguments: {}", promptName, arguments);
        
        if ("code_generation".equals(promptName)) {
            String language = (String) arguments.getOrDefault("language", "Java");
            String description = (String) arguments.getOrDefault("description", "");
            
            return Map.of(
                "description", "Code generation prompt",
                "messages", List.of(
                    Map.of(
                        "role", "user",
                        "content", Map.of("type", "text", 
                            "text", "请用 " + language + " 生成代码，实现以下功能：\n" + description)
                    )
                )
            );
        }
        
        throw new RuntimeException("Unknown prompt: " + promptName);
    }

    // ========== 初始化方法 ==========

    @SuppressWarnings("unchecked")
    private Object initialize(Object params) {
        Map<String, Object> paramsMap = params != null ? (Map<String, Object>) params : Map.of();
        log.info("Initializing MCP Streamable HTTP server with params: {}", paramsMap);
        
        return Map.of(
            "protocolVersion", "2024-11-05",
            "capabilities", Map.of(
                "tools", Map.of("streaming", true),
                "resources", Map.of("subscribe", true, "streaming", true),
                "prompts", Map.of()
            ),
            "serverInfo", Map.of(
                "name", "MCP Streamable HTTP Server",
                "version", "1.0.0"
            )
        );
    }

    // ========== 长任务示例 ==========

    @SuppressWarnings("unchecked")
    private Flux<McpMessage> executeLongTask(String requestId, Object params) {
        Map<String, Object> paramsMap = params != null ? (Map<String, Object>) params : Map.of();
        int steps = (int) paramsMap.getOrDefault("steps", 5);
        
        log.info("Executing long task with {} steps", steps);
        
        return Flux.interval(Duration.ofMillis(500))
            .take(steps + 1)
            .map(i -> {
                if (i < steps) {
                    // 进度更新
                    return createProgress(requestId, 
                        (int)((i + 1) * 100 / steps), 
                        "Executing step " + (i + 1) + "/" + steps);
                } else {
                    // 最终结果
                    return createResponse(requestId, Map.of(
                        "status", "completed",
                        "result", "Long task finished successfully"
                    ));
                }
            });
    }
}
