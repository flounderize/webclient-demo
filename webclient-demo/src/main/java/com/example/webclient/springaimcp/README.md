# Spring AI MCP 参考实现

## 简介

这是基于 Model Context Protocol (MCP) 官方规范的参考实现，使用 Spring WebFlux 和 Project Reactor 构建。

**重要说明：** 本实现不依赖任何外部 MCP 库，而是根据 MCP 协议规范从零实现。这使得它可以作为理解 MCP 协议的学习材料，也可以直接用于生产环境。

## 包结构

```
com.example.webclient.springaimcp/
├── client/                          # MCP 客户端实现
│   ├── McpStdioClient.java         # STDIO 传输客户端
│   ├── McpSseTransportClient.java  # SSE 传输客户端
│   └── McpStreamableHttpClient.java # Streamable HTTP 传输客户端
├── server/                          # MCP 服务器实现
│   ├── McpStdioServer.java         # STDIO 传输服务器
│   ├── McpSseServer.java           # SSE 传输服务器
│   ├── McpStreamableHttpServer.java # Streamable HTTP 传输服务器
│   └── SseReceiverController.java  # SSE 接收器示例
└── example/                         # 使用示例
    └── McpExampleController.java   # 完整的使用示例
```

## 核心特性

### 1. 完整的 MCP 协议支持

- ✅ JSON-RPC 2.0 消息格式
- ✅ 三种官方传输方式（STDIO、SSE、Streamable HTTP）
- ✅ 工具调用 (Tools)
- ✅ 资源管理 (Resources)
- ✅ 提示词管理 (Prompts)
- ✅ 初始化握手
- ✅ 错误处理
- ✅ 进度报告

### 2. 三种传输方式

#### STDIO 传输
- 基于标准输入输出的进程间通信
- 适用于本地工具调用和命令行集成
- 零网络配置，进程级隔离

#### SSE 传输
- 基于 Server-Sent Events 的实时推送
- 支持完整的握手流程
- 自动重连机制
- 适用于 Web 应用和远程服务

#### Streamable HTTP 传输
- 基于 HTTP Chunked Transfer Encoding
- 支持双向流式通信
- 标准 RESTful API 风格
- 适用于长时间运行任务和大数据传输

### 3. 响应式编程

- 完全基于 Project Reactor
- 支持背压控制
- 非阻塞 I/O
- 流式数据处理

## 快速开始

### 1. Streamable HTTP 客户端（最简单）

```java
@Autowired
private McpStreamableHttpClient client;

// 调用工具
Map<String, Object> args = Map.of("query", "MCP examples");
client.callTool("/api/springai/mcp/stream", "search", args)
    .subscribe(result -> System.out.println(result));

// 流式调用（获取中间进度）
client.callToolStreaming("/api/springai/mcp/stream", "search", args)
    .subscribe(chunk -> System.out.println("Chunk: " + chunk));

// 长任务（带进度回调）
client.sendWithProgress(
    "/api/springai/mcp/stream",
    "long_task",
    Map.of("steps", 10),
    progress -> System.out.println("Progress: " + progress)
).subscribe();
```

### 2. SSE 客户端

```java
@Autowired
private McpSseTransportClient client;

// 连接并握手
Map<String, Object> info = client.connect("/api/springai/mcp/sse").block();
String endpoint = (String) info.get("endpoint");

// 调用工具
client.callTool(endpoint, "get_weather", Map.of("city", "Beijing"))
    .subscribe(result -> System.out.println(result));

// 订阅通知
client.subscribeNotifications()
    .subscribe(notification -> 
        System.out.println("Notification: " + notification.getMethod())
    );
```

### 3. STDIO 客户端

```java
@Autowired
private McpStdioClient client;

// 启动 MCP 服务器进程
client.connect("node mcp-server.js").block();

// 调用工具
client.callTool("echo", Map.of("text", "Hello!"))
    .subscribe(result -> System.out.println(result));

// 读取资源
client.readResource("file:///data/file.txt")
    .subscribe(content -> System.out.println(content));

// 关闭连接
client.disconnect().block();
```

## 服务器实现

### Streamable HTTP 服务器

服务器自动启动，无需额外配置。端点：`/api/springai/mcp/stream`

测试：
```bash
curl -X POST -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":"1","method":"tools/list"}' \
  http://localhost:8080/api/springai/mcp/stream
```

### SSE 服务器

端点：
- SSE 连接：`GET /api/springai/mcp/sse`
- 发送请求：`POST /api/springai/mcp/sse/message`

测试：
```bash
# 建立 SSE 连接
curl -N -H "X-Session-Id: test-session" \
  http://localhost:8080/api/springai/mcp/sse

# 在另一个终端发送请求
curl -X POST -H "X-Session-Id: test-session" \
  -H "Content-Type: application/json" \
  -d '{"jsonrpc":"2.0","id":"1","method":"tools/list"}' \
  http://localhost:8080/api/springai/mcp/sse/message
```

### STDIO 服务器

```java
@Autowired
private McpStdioServer server;

// 启动服务器（监听标准输入）
server.start();

// 注册自定义方法
server.registerMethod("custom_method", (id, params) -> {
    return Map.of("result", "custom value");
});
```

## 示例 API

所有示例都在 `McpExampleController` 中，可以通过 HTTP 调用测试：

### 健康检查
```bash
curl http://localhost:8080/api/examples/mcp/health
```

### 工具调用
```bash
curl -X POST -H "Content-Type: application/json" \
  -d '{"tool":"search","arguments":{"query":"test"}}' \
  http://localhost:8080/api/examples/mcp/stream/tool/call
```

### 长任务（观察进度）
```bash
curl -X POST \
  http://localhost:8080/api/examples/mcp/stream/long-task?steps=10
```

### 提示词管理
```bash
curl http://localhost:8080/api/examples/mcp/prompts/comprehensive
```

## 与纯 WebClient SSE 的区别

| 特性 | 纯 WebClient SSE | Spring AI MCP 参考实现 |
|------|-----------------|---------------------|
| 位置 | `com.example.webclient.mcp` | `com.example.webclient.springaimcp` |
| 协议 | 简化版自定义协议 | 完整 MCP 规范 |
| 握手流程 | 无 | 有（SSE 模式） |
| 传输方式 | 仅 SSE + Stream | STDIO + SSE + Streamable HTTP |
| 消息格式 | 自定义 | JSON-RPC 2.0 |
| 工具调用 | 基础支持 | 完整支持（含 schema） |
| 资源管理 | 无 | 完整支持 |
| 提示词 | 无 | 完整支持 |
| 适用场景 | 快速原型 | 生产环境 |

## SSE 接收器

`SseReceiverController` 展示了如何在服务端接收和处理 SSE 流：

```java
// 接收 SSE 流
@PostMapping(value = "/receive", consumes = MediaType.TEXT_EVENT_STREAM_VALUE)
public Mono<Map<String, Object>> receiveSseStream(
    @RequestBody Flux<ServerSentEvent<String>> events)

// 聚合多个 SSE 源
@PostMapping("/aggregate/{sourceId}")
public Mono<Map<String, Object>> aggregateSseSource(
    @PathVariable String sourceId,
    @RequestBody Flux<ServerSentEvent<String>> events)
```

这与客户端 SSE（WebClient 订阅）不同，展示了服务端处理 SSE 的场景。

## 最佳实践

### 选择传输方式

1. **优先使用 Streamable HTTP**
   - 最简单的 HTTP 集成
   - RESTful API 风格
   - 无需长连接管理

2. **使用 SSE 当你需要**
   - 服务器主动推送
   - 实时通知
   - 长连接场景

3. **使用 STDIO 当你需要**
   - 本地工具调用
   - 命令行集成
   - 进程隔离

### 错误处理

```java
// 超时处理
client.callTool(endpoint, tool, args)
    .timeout(Duration.ofSeconds(30))
    .onErrorResume(TimeoutException.class, e -> {
        return Mono.just(Map.of("error", "timeout"));
    });

// 重试
client.connect(endpoint)
    .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)));
```

### 资源管理

```java
// STDIO 和 SSE 需要手动关闭
try {
    client.connect(endpoint).block();
    // 使用客户端
} finally {
    client.disconnect().block();
}

// Streamable HTTP 无需手动管理
```

## 参考资料

- [MCP 官方规范](https://modelcontextprotocol.io/)
- [详细实现指南](../../docs/mcp-implementation-guide.md)
- [Spring WebFlux 文档](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- [Project Reactor 文档](https://projectreactor.io/docs/core/release/reference/)

## 许可证

MIT License
