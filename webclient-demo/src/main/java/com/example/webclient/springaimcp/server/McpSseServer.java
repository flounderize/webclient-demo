package com.example.webclient.springaimcp.server;

import com.example.webclient.mcp.McpMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP SSE 传输服务器（基于 spring-ai-mcp 规范）
 * 
 * <p>通过 Server-Sent Events (SSE) 提供 MCP 服务。
 * 这是 MCP 协议的官方服务器实现之一，特别适用于：
 * <ul>
 *   <li>远程 HTTP 服务</li>
 *   <li>需要服务器主动推送的场景</li>
 *   <li>Web 应用集成</li>
 * </ul>
 * 
 * <p>MCP SSE 握手流程：
 * <ol>
 *   <li>客户端发起 SSE 连接到 /mcp/sse</li>
 *   <li>服务器返回 endpoint 事件，包含用于发送请求的 HTTP 端点</li>
 *   <li>客户端通过 POST /mcp/message 发送请求</li>
 *   <li>服务器通过 SSE 推送响应</li>
 * </ol>
 * 
 * @author AI Agent
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/springai/mcp/sse")
public class McpSseServer {

    private static final Logger log = LoggerFactory.getLogger(McpSseServer.class);

    private final ObjectMapper objectMapper;
    private final Map<String, Sinks.Many<ServerSentEvent<String>>> sessionSinks = new ConcurrentHashMap<>();

    public McpSseServer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * SSE 连接端点
     * 
     * <p>客户端连接此端点以接收服务器推送的消息
     * 
     * @param sessionId 会话ID（从请求头获取）
     * @return SSE 事件流
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> connect(@RequestHeader("X-Session-Id") String sessionId) {
        log.info("MCP SSE client connected - session: {}", sessionId);

        // 创建会话专用的 Sink
        Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().multicast().onBackpressureBuffer();
        sessionSinks.put(sessionId, sink);

        // 发送 endpoint 事件（握手）
        try {
            Map<String, Object> endpointInfo = Map.of(
                "endpoint", "/api/springai/mcp/sse/message",
                "sessionId", sessionId,
                "protocolVersion", "2024-11-05"
            );
            
            String endpointJson = objectMapper.writeValueAsString(endpointInfo);
            
            ServerSentEvent<String> endpointEvent = ServerSentEvent.<String>builder()
                .event("endpoint")
                .data(endpointJson)
                .build();
            
            sink.tryEmitNext(endpointEvent);
        } catch (Exception e) {
            log.error("Failed to send endpoint event", e);
        }

        // 定期发送心跳
        Flux<ServerSentEvent<String>> heartbeat = Flux.interval(Duration.ofSeconds(30))
            .map(seq -> ServerSentEvent.<String>builder()
                .event("heartbeat")
                .data("{}")
                .build());

        return Flux.merge(sink.asFlux(), heartbeat)
            .doOnCancel(() -> {
                log.info("MCP SSE client disconnected - session: {}", sessionId);
                sessionSinks.remove(sessionId);
            })
            .doOnError(e -> {
                log.error("Error in SSE stream - session: {}", sessionId, e);
                sessionSinks.remove(sessionId);
            });
    }

    /**
     * 接收客户端请求并通过 SSE 推送响应
     * 
     * @param sessionId 会话ID
     * @param request MCP 请求
     * @return 确认响应
     */
    @PostMapping("/message")
    public Mono<Map<String, Object>> handleMessage(
            @RequestHeader("X-Session-Id") String sessionId,
            @RequestBody McpMessage request) {
        
        log.info("Received MCP SSE request - session: {}, id: {}, method: {}", 
            sessionId, request.getId(), request.getMethod());

        // 异步处理请求并推送响应
        processRequest(sessionId, request)
            .subscribe(
                response -> sendMessage(sessionId, response),
                error -> {
                    log.error("Error processing request", error);
                    sendError(sessionId, request.getId(), -32603, "Internal error: " + error.getMessage());
                }
            );

        // 立即返回确认
        return Mono.just(Map.of("status", "accepted", "requestId", request.getId()));
    }

    /**
     * 处理请求并生成响应
     */
    private Mono<McpMessage> processRequest(String sessionId, McpMessage request) {
        String method = request.getMethod();
        Object params = request.getParams();
        
        return Mono.fromCallable(() -> {
            Object result = switch (method) {
                case "tools/list" -> listTools();
                case "tools/call" -> callTool(params);
                case "resources/list" -> listResources();
                case "resources/read" -> readResource(params);
                case "prompts/list" -> listPrompts();
                case "prompts/get" -> getPrompt(params);
                case "initialize" -> initialize(params);
                default -> throw new RuntimeException("Unknown method: " + method);
            };
            
            McpMessage response = new McpMessage();
            response.setId(request.getId());
            response.setResult(result);
            return response;
        });
    }

    /**
     * 发送消息到客户端
     */
    private void sendMessage(String sessionId, McpMessage message) {
        Sinks.Many<ServerSentEvent<String>> sink = sessionSinks.get(sessionId);
        if (sink == null) {
            log.warn("Session not found: {}", sessionId);
            return;
        }

        try {
            String messageJson = objectMapper.writeValueAsString(message);
            ServerSentEvent<String> event = ServerSentEvent.<String>builder()
                .event("message")
                .id(message.getId())
                .data(messageJson)
                .build();
            
            sink.tryEmitNext(event);
            log.debug("Sent MCP SSE message - session: {}, id: {}", sessionId, message.getId());
        } catch (Exception e) {
            log.error("Failed to send message", e);
        }
    }

    /**
     * 发送错误到客户端
     */
    private void sendError(String sessionId, String requestId, int code, String message) {
        McpMessage errorResponse = new McpMessage();
        errorResponse.setId(requestId);
        errorResponse.setError(new McpMessage.McpError(code, message));
        sendMessage(sessionId, errorResponse);
    }

    /**
     * 发送通知到客户端（无请求ID的消息）
     */
    public void sendNotification(String sessionId, String method, Object params) {
        McpMessage notification = new McpMessage();
        notification.setMethod(method);
        notification.setParams(params);
        sendMessage(sessionId, notification);
    }

    // ========== 工具相关方法实现 ==========

    private Object listTools() {
        return Map.of(
            "tools", List.of(
                Map.of(
                    "name", "echo",
                    "description", "回显输入的文本",
                    "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "text", Map.of("type", "string", "description", "要回显的文本")
                        ),
                        "required", List.of("text")
                    )
                ),
                Map.of(
                    "name", "get_weather",
                    "description", "获取天气信息",
                    "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "city", Map.of("type", "string", "description", "城市名称")
                        ),
                        "required", List.of("city")
                    )
                )
            )
        );
    }

    @SuppressWarnings("unchecked")
    private Object callTool(Object params) {
        Map<String, Object> paramsMap = (Map<String, Object>) params;
        String toolName = (String) paramsMap.get("name");
        Map<String, Object> arguments = (Map<String, Object>) paramsMap.get("arguments");
        
        log.info("Calling tool: {} with arguments: {}", toolName, arguments);
        
        return switch (toolName) {
            case "echo" -> Map.of("content", List.of(
                Map.of("type", "text", "text", arguments.get("text"))
            ));
            case "get_weather" -> {
                String city = (String) arguments.get("city");
                yield Map.of("content", List.of(
                    Map.of("type", "text", "text", city + " 的天气：晴天，温度 22°C")
                ));
            }
            default -> throw new RuntimeException("Unknown tool: " + toolName);
        };
    }

    // ========== 资源相关方法实现 ==========

    private Object listResources() {
        return Map.of(
            "resources", List.of(
                Map.of(
                    "uri", "file:///logs/app.log",
                    "name", "Application Logs",
                    "description", "应用程序日志",
                    "mimeType", "text/plain"
                ),
                Map.of(
                    "uri", "file:///docs/api.md",
                    "name", "API Documentation",
                    "description", "API 文档",
                    "mimeType", "text/markdown"
                )
            )
        );
    }

    @SuppressWarnings("unchecked")
    private Object readResource(Object params) {
        Map<String, Object> paramsMap = (Map<String, Object>) params;
        String uri = (String) paramsMap.get("uri");
        
        log.info("Reading resource: {}", uri);
        
        return Map.of(
            "contents", List.of(
                Map.of(
                    "uri", uri,
                    "mimeType", "text/plain",
                    "text", "This is the content of " + uri
                )
            )
        );
    }

    // ========== 提示词相关方法实现 ==========

    private Object listPrompts() {
        return Map.of(
            "prompts", List.of(
                Map.of(
                    "name", "translate",
                    "description", "翻译文本提示词",
                    "arguments", List.of(
                        Map.of("name", "target_language", "description", "目标语言", "required", true)
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
        
        if ("translate".equals(promptName)) {
            String targetLanguage = (String) arguments.getOrDefault("target_language", "English");
            return Map.of(
                "description", "Translation prompt",
                "messages", List.of(
                    Map.of(
                        "role", "user",
                        "content", Map.of("type", "text", 
                            "text", "请将以下文本翻译成 " + targetLanguage + "：\n\n{text}")
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
        log.info("Initializing MCP SSE server with params: {}", paramsMap);
        
        return Map.of(
            "protocolVersion", "2024-11-05",
            "capabilities", Map.of(
                "tools", Map.of(),
                "resources", Map.of("subscribe", true),
                "prompts", Map.of()
            ),
            "serverInfo", Map.of(
                "name", "MCP SSE Server",
                "version", "1.0.0"
            )
        );
    }
}
