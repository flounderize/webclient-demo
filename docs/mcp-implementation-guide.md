# Spring AI MCP 参考实现指南

## 概述

本文档详细说明了基于 Model Context Protocol (MCP) 规范的参考实现，包括三种传输方式，以及它们与纯 WebClient SSE 实现的区别。

**注意：** 本实现是基于 MCP 规范的参考实现，不依赖 spring-ai-mcp 库（该库尚未发布）。所有代码都是使用 Spring WebFlux 和 Reactor 从零实现的标准 MCP 协议。

## MCP 协议简介

Model Context Protocol (MCP) 是一个用于 AI 模型与工具、资源交互的标准化协议。它基于 JSON-RPC 2.0，支持多种传输方式。

### MCP 核心能力

1. **工具调用 (Tools)** - 让 AI 模型能够调用外部工具
2. **资源读取 (Resources)** - 访问文件、数据库等资源
3. **提示词管理 (Prompts)** - 管理和获取提示词模板

## 三种传输方式

### 1. STDIO 传输

**适用场景：**
- 本地进程间通信
- 命令行工具集成
- 子进程管理
- 需要简单部署的场景

**实现类：**
- 客户端：`McpStdioClient`
- 服务端：`McpStdioServer`

**特点：**
- 基于标准输入输出
- 进程级隔离
- 无需网络配置
- 适合本地工具调用

**使用示例：**

```java
@Autowired
private McpStdioClient stdioClient;

// 启动 MCP 服务器进程
stdioClient.connect("node mcp-server.js").block();

// 调用工具
Map<String, Object> arguments = Map.of("text", "Hello, MCP!");
Object result = stdioClient.callTool("echo", arguments).block();

// 读取资源
Object resource = stdioClient.readResource("file:///data/file.txt").block();

// 获取提示词
Map<String, Object> promptArgs = Map.of("language", "Java");
Object prompt = stdioClient.getPrompt("code_review", promptArgs).block();

// 关闭连接
stdioClient.disconnect().block();
```

**服务端示例：**

```java
@Autowired
private McpStdioServer stdioServer;

// 启动服务器
stdioServer.start();

// 注册自定义工具
stdioServer.registerMethod("custom_tool", (requestId, params) -> {
    // 处理逻辑
    return Map.of("result", "custom result");
});

// 停止服务器
stdioServer.stop();
```

### 2. SSE 传输

**适用场景：**
- 远程 HTTP 服务
- 需要服务器主动推送
- Web 应用集成
- 实时通知场景

**实现类：**
- 客户端：`McpSseTransportClient`
- 服务端：`McpSseServer`

**特点：**
- 基于 Server-Sent Events
- 支持服务器推送
- 自动重连机制
- HTTP 协议友好

**MCP SSE 握手流程：**

1. 客户端发起 SSE 连接到 `/api/springai/mcp/sse`
2. 服务器返回 `endpoint` 事件，包含用于发送请求的 HTTP 端点
3. 客户端通过 POST `/api/springai/mcp/sse/message` 发送请求
4. 服务器通过 SSE 推送响应

**使用示例：**

```java
@Autowired
private McpSseTransportClient sseClient;

// 建立连接并完成握手
Map<String, Object> handshakeInfo = sseClient.connect("/api/springai/mcp/sse").block();
String requestEndpoint = (String) handshakeInfo.get("endpoint");

// 调用工具
Map<String, Object> arguments = Map.of("city", "Beijing");
Object result = sseClient.callTool(requestEndpoint, "get_weather", arguments).block();

// 订阅服务器通知
Flux<McpMessage> notifications = sseClient.subscribeNotifications();
notifications.subscribe(msg -> 
    System.out.println("Notification: " + msg.getMethod())
);

// 读取资源
Object resource = sseClient.readResource(requestEndpoint, "file:///logs/app.log").block();

// 关闭连接
sseClient.disconnect().block();
```

**服务端示例：**

客户端连接：
```bash
curl -N -H "X-Session-Id: my-session" http://localhost:8080/api/springai/mcp/sse
```

发送请求：
```bash
curl -X POST -H "X-Session-Id: my-session" \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":"1","method":"tools/list"}' \
  http://localhost:8080/api/springai/mcp/sse/message
```

### 3. Streamable HTTP 传输

**适用场景：**
- 双向流式通信
- 标准 HTTP/REST 架构
- 长时间运行任务进度报告
- 大数据流式处理

**实现类：**
- 客户端：`McpStreamableHttpClient`
- 服务端：`McpStreamableHttpServer`

**特点：**
- 基于 HTTP Chunked Transfer Encoding
- 请求-响应模式
- 支持中间进度报告
- 每个请求独立连接

**使用示例：**

```java
@Autowired
private McpStreamableHttpClient streamClient;

// 调用工具（流式）
Flux<Object> results = streamClient.callToolStreaming(
    "/api/springai/mcp/stream",
    "search",
    Map.of("query", "MCP examples")
);
results.subscribe(chunk -> System.out.println("Chunk: " + chunk));

// 调用工具（带进度回调）
Mono<McpMessage> result = streamClient.sendWithProgress(
    "/api/springai/mcp/stream",
    "tools/call",
    Map.of("name", "analyze", "arguments", Map.of("data", "large dataset")),
    progress -> System.out.println("Progress: " + progress.getResult())
);

// 读取大文件（流式）
Flux<Object> resourceChunks = streamClient.readResourceStreaming(
    "/api/springai/mcp/stream",
    "file:///data/large_file.txt"
);

// 执行长任务
Flux<McpMessage> taskProgress = streamClient.sendStreamRequest(
    "/api/springai/mcp/stream",
    "long_task",
    Map.of("steps", 10)
);
taskProgress.subscribe(msg -> {
    if ("progress".equals(msg.getMethod())) {
        System.out.println("Progress: " + msg.getResult());
    } else {
        System.out.println("Final result: " + msg.getResult());
    }
});

// 批量调用工具
List<Map<String, Object>> toolCalls = List.of(
    Map.of("name", "echo", "arguments", Map.of("text", "test1")),
    Map.of("name", "echo", "arguments", Map.of("text", "test2"))
);
Flux<Object> batchResults = streamClient.callToolsBatch(
    "/api/springai/mcp/stream",
    toolCalls
);
```

**服务端测试：**

```bash
# 调用工具
curl -X POST -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":"1","method":"tools/call","params":{"name":"search","arguments":{"query":"test"}}}' \
  http://localhost:8080/api/springai/mcp/stream

# 执行长任务
curl -X POST -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":"2","method":"long_task","params":{"steps":5}}' \
  http://localhost:8080/api/springai/mcp/stream
```

## 与纯 WebClient SSE 的区别

### 纯 WebClient SSE 实现

位于 `com.example.webclient.mcp` 包下的 `McpSseClient` 和 `McpStreamClient`。

**特点：**
- 通用的 SSE/Stream 客户端
- 不严格遵循 MCP 协议规范
- 简单的消息收发
- 无握手流程

### Spring AI MCP 实现

位于 `com.example.webclient.springaimcp` 包下。

**特点：**
- 严格遵循 MCP 协议规范
- 实现完整的握手流程
- 支持三种官方传输方式
- 实现 JSON-RPC 2.0 格式
- 支持工具、资源、提示词三种能力
- 更好的类型安全和错误处理

### 对比表

| 特性 | 纯 WebClient SSE | Spring AI MCP |
|------|-----------------|---------------|
| 协议规范 | 简化版 | 完整 MCP 协议 |
| 握手流程 | 无 | 有（SSE 模式） |
| 传输方式 | SSE + Stream | STDIO + SSE + Streamable HTTP |
| 消息格式 | 自定义 | JSON-RPC 2.0 |
| 工具调用 | 基础支持 | 完整支持（含 schema） |
| 资源管理 | 无 | 完整支持 |
| 提示词管理 | 无 | 完整支持 |
| 进度报告 | 基础支持 | 标准化支持 |
| 适用场景 | 快速原型 | 生产环境 |

## Spring WebFlux SSE 接收器

位于 `SseReceiverController` 和 `SseDataProcessor`。

**用途：**
展示服务端如何接收和处理 SSE 数据流。

**与客户端 SSE 的区别：**
- **客户端 SSE（WebClient）**：订阅远程 SSE 端点，接收服务器推送
- **服务端 SSE（WebFlux）**：暴露端点接收 SSE 数据，进行处理和转发

**使用场景：**
- SSE 数据聚合
- SSE 事件转发
- SSE 数据持久化
- 多源 SSE 合并

**示例：**

```java
@Autowired
private WebClient webClient;

// 发送 SSE 流到服务器
Flux<ServerSentEvent<String>> events = Flux.interval(Duration.ofSeconds(1))
    .take(5)
    .map(i -> ServerSentEvent.<String>builder()
        .event("message")
        .data("{\"index\":" + i + "}")
        .build());

Map<String, Object> result = webClient.post()
    .uri("/api/springai/sse/receive")
    .contentType(MediaType.TEXT_EVENT_STREAM)
    .body(events, new ParameterizedTypeReference<ServerSentEvent<String>>() {})
    .retrieve()
    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
    .block();

System.out.println("Result: " + result);
```

## 最佳实践

### 选择传输方式

1. **使用 STDIO**：
   - 本地工具调用
   - 命令行应用
   - 进程隔离需求

2. **使用 SSE**：
   - 需要服务器推送
   - 实时通知场景
   - Web 应用集成

3. **使用 Streamable HTTP**：
   - RESTful API 风格
   - 需要进度报告
   - 大数据流式处理
   - 最简单的 HTTP 集成

### 错误处理

```java
// 使用超时
streamClient.callTool(endpoint, tool, args)
    .timeout(Duration.ofSeconds(30))
    .onErrorResume(TimeoutException.class, e -> {
        log.error("Request timeout", e);
        return Mono.just(Map.of("error", "timeout"));
    });

// 使用重试
sseClient.connect(endpoint)
    .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
    .subscribe();

// 错误转换
stdioClient.callTool(tool, args)
    .onErrorMap(e -> new McpException("Tool call failed", e));
```

### 资源管理

```java
// STDIO：记得关闭连接
try {
    stdioClient.connect(command).block();
    // 使用客户端
} finally {
    stdioClient.disconnect().block();
}

// SSE：使用 try-with-resources 模式
try {
    sseClient.connect(endpoint).block();
    // 使用客户端
} finally {
    sseClient.disconnect().block();
}

// Streamable HTTP：无需手动管理连接
```

### 性能优化

```java
// 使用背压控制
streamClient.sendWithBackpressure(endpoint, method, params, 10)
    .subscribe();

// 批量操作
List<Map<String, Object>> calls = // ... 多个工具调用
streamClient.callToolsBatch(endpoint, calls)
    .subscribe();

// 并发控制
Flux.fromIterable(items)
    .flatMap(item -> streamClient.callTool(endpoint, tool, item), 3)
    .subscribe();
```

## 示例端点

### 测试 Streamable HTTP

```bash
# 健康检查
curl http://localhost:8080/api/examples/mcp/health

# 列出工具
curl -X POST -H "Content-Type: application/json" \
  -d '{"tool":"search","arguments":{"query":"test"}}' \
  http://localhost:8080/api/examples/mcp/stream/tool/call

# 长任务（观察进度）
curl -X POST http://localhost:8080/api/examples/mcp/stream/long-task?steps=10
```

### 测试 SSE 接收器

```bash
# 接收 SSE 流（需要使用支持 SSE 的客户端）
# 这需要特殊的客户端，例如 curl 配合 --no-buffer
```

## 总结

1. **STDIO** - 最适合本地工具和命令行集成
2. **SSE** - 最适合需要服务器推送的 Web 应用
3. **Streamable HTTP** - 最适合标准 RESTful API 和流式处理
4. **Spring AI MCP** 实现提供了完整的 MCP 协议支持
5. **纯 WebClient** 实现适合快速原型和简单场景
6. **WebFlux SSE 接收器** 展示了服务端处理 SSE 的方式

选择合适的传输方式取决于你的具体需求和部署环境。
