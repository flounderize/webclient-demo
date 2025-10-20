package com.example.webclient.springaimcp.client;

import com.example.webclient.mcp.McpMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.io.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * MCP STDIO 传输客户端
 * 
 * <p>使用标准输入输出（STDIO）与 MCP 服务器通信。
 * 这是 MCP 协议的标准传输方式之一，特别适用于：
 * <ul>
 *   <li>本地进程间通信</li>
 *   <li>命令行工具集成</li>
 *   <li>子进程管理</li>
 * </ul>
 * 
 * <p>实现特点：
 * <ul>
 *   <li>基于进程的双向通信</li>
 *   <li>JSON-RPC 2.0 消息格式</li>
 *   <li>请求ID关联响应</li>
 *   <li>异步消息处理</li>
 * </ul>
 * 
 * @author AI Agent
 * @since 1.0.0
 */
@Component
public class McpStdioClient {

    private static final Logger log = LoggerFactory.getLogger(McpStdioClient.class);

    private final ObjectMapper objectMapper;
    private final Map<String, Sinks.One<McpMessage>> pendingRequests = new ConcurrentHashMap<>();
    private Process serverProcess;
    private BufferedReader reader;
    private BufferedWriter writer;

    public McpStdioClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 启动 MCP 服务器进程并建立 STDIO 连接
     * 
     * @param command 启动服务器的命令（例如：node server.js 或 python server.py）
     * @return Mono 表示连接建立完成
     */
    public Mono<Void> connect(String command) {
        return Mono.fromRunnable(() -> {
            try {
                log.info("Starting MCP server with command: {}", command);
                
                // 启动服务器进程
                ProcessBuilder processBuilder = new ProcessBuilder(command.split("\\s+"));
                processBuilder.redirectErrorStream(true);
                serverProcess = processBuilder.start();
                
                // 获取输入输出流
                reader = new BufferedReader(new InputStreamReader(serverProcess.getInputStream()));
                writer = new BufferedWriter(new OutputStreamWriter(serverProcess.getOutputStream()));
                
                // 启动消息读取线程
                startMessageReader();
                
                log.info("MCP STDIO connection established");
            } catch (IOException e) {
                log.error("Failed to start MCP server", e);
                throw new RuntimeException("Failed to start MCP server", e);
            }
        });
    }

    /**
     * 发送 MCP 请求并等待响应
     * 
     * @param method 方法名
     * @param params 参数
     * @return Mono 包装的响应消息
     */
    public Mono<McpMessage> sendRequest(String method, Object params) {
        String requestId = UUID.randomUUID().toString();
        log.info("Sending MCP STDIO request - id: {}, method: {}", requestId, method);

        McpMessage request = new McpMessage(requestId, method, params);
        
        // 创建响应接收器
        Sinks.One<McpMessage> responseSink = Sinks.one();
        pendingRequests.put(requestId, responseSink);

        return Mono.fromRunnable(() -> {
            try {
                // 序列化并发送请求
                String json = objectMapper.writeValueAsString(request);
                writer.write(json);
                writer.newLine();
                writer.flush();
                log.debug("Sent MCP STDIO request: {}", json);
            } catch (IOException e) {
                log.error("Failed to send MCP request", e);
                responseSink.tryEmitError(e);
                pendingRequests.remove(requestId);
            }
        })
        .then(responseSink.asMono())
        .timeout(java.time.Duration.ofSeconds(30))
        .doOnError(TimeoutException.class, e -> {
            log.error("MCP request timeout - id: {}", requestId);
            pendingRequests.remove(requestId);
        });
    }

    /**
     * 调用工具
     * 
     * @param toolName 工具名称
     * @param arguments 工具参数
     * @return Mono 包装的工具调用结果
     */
    public Mono<Object> callTool(String toolName, Map<String, Object> arguments) {
        log.info("Calling tool: {} with arguments: {}", toolName, arguments);
        
        Map<String, Object> params = Map.of(
            "name", toolName,
            "arguments", arguments
        );
        
        return sendRequest("tools/call", params)
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
     * @return Mono 包装的工具列表
     */
    public Mono<Object> listTools() {
        log.info("Listing available tools");
        return sendRequest("tools/list", null)
            .map(McpMessage::getResult);
    }

    /**
     * 读取资源
     * 
     * @param uri 资源URI
     * @return Mono 包装的资源内容
     */
    public Mono<Object> readResource(String uri) {
        log.info("Reading resource: {}", uri);
        
        Map<String, Object> params = Map.of("uri", uri);
        
        return sendRequest("resources/read", params)
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
     * @return Mono 包装的资源列表
     */
    public Mono<Object> listResources() {
        log.info("Listing available resources");
        return sendRequest("resources/list", null)
            .map(McpMessage::getResult);
    }

    /**
     * 获取提示词
     * 
     * @param promptName 提示词名称
     * @param arguments 提示词参数
     * @return Mono 包装的提示词内容
     */
    public Mono<Object> getPrompt(String promptName, Map<String, Object> arguments) {
        log.info("Getting prompt: {} with arguments: {}", promptName, arguments);
        
        Map<String, Object> params = Map.of(
            "name", promptName,
            "arguments", arguments != null ? arguments : Map.of()
        );
        
        return sendRequest("prompts/get", params)
            .map(response -> {
                if (response.getError() != null) {
                    throw new RuntimeException("Prompt get failed: " + response.getError().getMessage());
                }
                return response.getResult();
            });
    }

    /**
     * 列出可用提示词
     * 
     * @return Mono 包装的提示词列表
     */
    public Mono<Object> listPrompts() {
        log.info("Listing available prompts");
        return sendRequest("prompts/list", null)
            .map(McpMessage::getResult);
    }

    /**
     * 关闭连接
     * 
     * @return Mono 表示关闭完成
     */
    public Mono<Void> disconnect() {
        return Mono.fromRunnable(() -> {
            try {
                log.info("Closing MCP STDIO connection");
                
                if (writer != null) {
                    writer.close();
                }
                if (reader != null) {
                    reader.close();
                }
                if (serverProcess != null) {
                    serverProcess.destroy();
                    serverProcess.waitFor();
                }
                
                pendingRequests.clear();
                log.info("MCP STDIO connection closed");
            } catch (Exception e) {
                log.error("Error closing MCP STDIO connection", e);
            }
        });
    }

    /**
     * 启动消息读取线程
     */
    private void startMessageReader() {
        Thread readerThread = new Thread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("Received MCP STDIO message: {}", line);
                    
                    try {
                        McpMessage message = objectMapper.readValue(line, McpMessage.class);
                        handleMessage(message);
                    } catch (Exception e) {
                        log.error("Failed to parse MCP message", e);
                    }
                }
            } catch (IOException e) {
                log.error("Error reading MCP messages", e);
            }
        });
        
        readerThread.setDaemon(true);
        readerThread.setName("mcp-stdio-reader");
        readerThread.start();
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
        } else if (message.getMethod() != null) {
            // 这是服务器主动发送的通知
            log.info("Received notification: {}", message.getMethod());
        }
    }
}
