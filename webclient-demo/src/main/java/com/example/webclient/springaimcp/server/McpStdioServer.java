package com.example.webclient.springaimcp.server;

import com.example.webclient.mcp.McpMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * MCP STDIO 传输服务器
 * 
 * <p>通过标准输入输出（STDIO）提供 MCP 服务。
 * 这是 MCP 协议的标准服务器实现之一，特别适用于：
 * <ul>
 *   <li>作为本地命令行工具运行</li>
 *   <li>被其他进程调用的服务</li>
 *   <li>需要简单部署的场景</li>
 * </ul>
 * 
 * <p>实现特点：
 * <ul>
 *   <li>基于 STDIO 的双向通信</li>
 *   <li>JSON-RPC 2.0 消息格式</li>
 *   <li>支持工具、资源、提示词三种能力</li>
 *   <li>可扩展的方法注册机制</li>
 * </ul>
 * 
 * @author AI Agent
 * @since 1.0.0
 */
@Component
public class McpStdioServer {

    private static final Logger log = LoggerFactory.getLogger(McpStdioServer.class);

    private final ObjectMapper objectMapper;
    private final Map<String, BiFunction<String, Object, Object>> methodHandlers = new ConcurrentHashMap<>();
    
    private BufferedReader reader;
    private BufferedWriter writer;
    private volatile boolean running = false;

    public McpStdioServer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        registerDefaultHandlers();
    }

    /**
     * 启动 MCP STDIO 服务器
     * 
     * <p>从标准输入读取请求，向标准输出写入响应
     */
    public void start() {
        log.info("Starting MCP STDIO server");
        
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        this.writer = new BufferedWriter(new OutputStreamWriter(System.out));
        this.running = true;
        
        // 启动消息处理循环
        startMessageLoop();
    }

    /**
     * 停止服务器
     */
    public void stop() {
        log.info("Stopping MCP STDIO server");
        running = false;
        
        try {
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();
            }
        } catch (IOException e) {
            log.error("Error closing STDIO streams", e);
        }
    }

    /**
     * 注册方法处理器
     * 
     * @param method 方法名
     * @param handler 处理器函数 (requestId, params) -> result
     */
    public void registerMethod(String method, BiFunction<String, Object, Object> handler) {
        log.info("Registering MCP method: {}", method);
        methodHandlers.put(method, handler);
    }

    /**
     * 注册默认的方法处理器
     */
    private void registerDefaultHandlers() {
        // 工具相关方法
        registerMethod("tools/list", (id, params) -> listTools());
        registerMethod("tools/call", (id, params) -> callTool(params));
        
        // 资源相关方法
        registerMethod("resources/list", (id, params) -> listResources());
        registerMethod("resources/read", (id, params) -> readResource(params));
        
        // 提示词相关方法
        registerMethod("prompts/list", (id, params) -> listPrompts());
        registerMethod("prompts/get", (id, params) -> getPrompt(params));
        
        // 初始化方法
        registerMethod("initialize", (id, params) -> initialize(params));
    }

    /**
     * 消息处理循环
     */
    private void startMessageLoop() {
        Thread loopThread = new Thread(() -> {
            try {
                String line;
                while (running && (line = reader.readLine()) != null) {
                    log.debug("Received MCP request: {}", line);
                    
                    try {
                        McpMessage request = objectMapper.readValue(line, McpMessage.class);
                        handleRequest(request);
                    } catch (Exception e) {
                        log.error("Failed to process MCP request", e);
                        sendError(null, -32700, "Parse error", null);
                    }
                }
            } catch (IOException e) {
                log.error("Error in message loop", e);
            }
        });
        
        loopThread.setName("mcp-stdio-server");
        loopThread.start();
    }

    /**
     * 处理请求
     */
    private void handleRequest(McpMessage request) {
        String requestId = request.getId();
        String method = request.getMethod();
        Object params = request.getParams();
        
        log.info("Handling MCP request - id: {}, method: {}", requestId, method);
        
        BiFunction<String, Object, Object> handler = methodHandlers.get(method);
        
        if (handler == null) {
            sendError(requestId, -32601, "Method not found: " + method, null);
            return;
        }
        
        try {
            Object result = handler.apply(requestId, params);
            sendResponse(requestId, result);
        } catch (Exception e) {
            log.error("Error handling method: {}", method, e);
            sendError(requestId, -32603, "Internal error: " + e.getMessage(), null);
        }
    }

    /**
     * 发送响应
     */
    private void sendResponse(String requestId, Object result) {
        try {
            McpMessage response = new McpMessage();
            response.setId(requestId);
            response.setResult(result);
            
            String json = objectMapper.writeValueAsString(response);
            synchronized (writer) {
                writer.write(json);
                writer.newLine();
                writer.flush();
            }
            
            log.debug("Sent MCP response: {}", json);
        } catch (IOException e) {
            log.error("Failed to send response", e);
        }
    }

    /**
     * 发送错误响应
     */
    private void sendError(String requestId, int code, String message, Object data) {
        try {
            McpMessage response = new McpMessage();
            response.setId(requestId);
            
            McpMessage.McpError error = new McpMessage.McpError(code, message);
            error.setData(data);
            response.setError(error);
            
            String json = objectMapper.writeValueAsString(response);
            synchronized (writer) {
                writer.write(json);
                writer.newLine();
                writer.flush();
            }
            
            log.debug("Sent MCP error: {}", json);
        } catch (IOException e) {
            log.error("Failed to send error", e);
        }
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
                    "name", "calculate",
                    "description", "执行数学计算",
                    "inputSchema", Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "expression", Map.of("type", "string", "description", "数学表达式")
                        ),
                        "required", List.of("expression")
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
        
        switch (toolName) {
            case "echo":
                return Map.of("content", List.of(
                    Map.of("type", "text", "text", arguments.get("text"))
                ));
            case "calculate":
                // 简单的计算示例
                String expression = (String) arguments.get("expression");
                return Map.of("content", List.of(
                    Map.of("type", "text", "text", "Result: " + expression + " = ?")
                ));
            default:
                throw new RuntimeException("Unknown tool: " + toolName);
        }
    }

    // ========== 资源相关方法实现 ==========

    private Object listResources() {
        return Map.of(
            "resources", List.of(
                Map.of(
                    "uri", "file:///example/data.txt",
                    "name", "Example Data",
                    "description", "示例数据文件",
                    "mimeType", "text/plain"
                ),
                Map.of(
                    "uri", "file:///example/config.json",
                    "name", "Configuration",
                    "description", "配置文件",
                    "mimeType", "application/json"
                )
            )
        );
    }

    @SuppressWarnings("unchecked")
    private Object readResource(Object params) {
        Map<String, Object> paramsMap = (Map<String, Object>) params;
        String uri = (String) paramsMap.get("uri");
        
        log.info("Reading resource: {}", uri);
        
        // 模拟资源读取
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
                    "name", "code_review",
                    "description", "代码审查提示词",
                    "arguments", List.of(
                        Map.of("name", "language", "description", "编程语言", "required", true)
                    )
                ),
                Map.of(
                    "name", "summarize",
                    "description", "文本摘要提示词",
                    "arguments", List.of(
                        Map.of("name", "length", "description", "摘要长度", "required", false)
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
        
        String promptText;
        if ("code_review".equals(promptName)) {
            String language = (String) arguments.getOrDefault("language", "unknown");
            promptText = "请审查以下 " + language + " 代码：\n\n{code}\n\n请指出潜在的问题和改进建议。";
        } else if ("summarize".equals(promptName)) {
            promptText = "请总结以下文本：\n\n{text}\n\n生成简洁的摘要。";
        } else {
            throw new RuntimeException("Unknown prompt: " + promptName);
        }
        
        return Map.of(
            "description", "Prompt for " + promptName,
            "messages", List.of(
                Map.of(
                    "role", "user",
                    "content", Map.of("type", "text", "text", promptText)
                )
            )
        );
    }

    // ========== 初始化方法 ==========

    @SuppressWarnings("unchecked")
    private Object initialize(Object params) {
        Map<String, Object> paramsMap = params != null ? (Map<String, Object>) params : Map.of();
        log.info("Initializing MCP server with params: {}", paramsMap);
        
        return Map.of(
            "protocolVersion", "2024-11-05",
            "capabilities", Map.of(
                "tools", Map.of(),
                "resources", Map.of("subscribe", true),
                "prompts", Map.of()
            ),
            "serverInfo", Map.of(
                "name", "MCP STDIO Server",
                "version", "1.0.0"
            )
        );
    }
}
