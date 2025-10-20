# WebClient Demo - Spring Boot WebClient 示例工程

## 项目简介

这是一个面向企业级 AI Agent 的 Spring Boot WebClient 示例工程，演示了 WebClient 在不同场景下的使用方式，包括：

- **同步调用** - 使用 `Mono.block()` 阻塞获取结果
- **异步调用** - 返回 `Mono`/`Flux` 供响应式编排
- **响应式流式调用** - 处理流式数据（NDJSON）
- **SSE 调用** - 订阅 Server-Sent Events
- **MCP 调用** - 支持 Model Context Protocol 完整实现
  - **STDIO 传输** - 进程间通信
  - **SSE 传输** - 服务器推送
  - **Streamable HTTP 传输** - 流式 HTTP

## 🎯 新增：Spring AI MCP 参考实现

本项目包含基于 **MCP (Model Context Protocol)** 规范的完整参考实现，支持三种官方传输方式：

### MCP 实现特点

- ✅ **完整的 JSON-RPC 2.0** 消息格式
- ✅ **三种传输方式** - STDIO、SSE、Streamable HTTP
- ✅ **工具调用** - 让 AI 调用外部工具
- ✅ **资源管理** - 访问文件、数据等资源
- ✅ **提示词管理** - 管理和获取提示词模板
- ✅ **流式进度报告** - 长任务实时进度
- ✅ **完整的握手流程** - SSE 传输模式
- ✅ **Spring WebFlux SSE 接收器** - 服务端接收 SSE 示例

### 快速体验 MCP

```bash
# 健康检查
curl http://localhost:8080/api/examples/mcp/health

# 调用工具
curl -X POST -H "Content-Type: application/json" \
  -d '{"tool":"search","arguments":{"query":"test"}}' \
  http://localhost:8080/api/examples/mcp/stream/tool/call

# 长任务（观察流式进度）
curl -X POST http://localhost:8080/api/examples/mcp/stream/long-task?steps=10
```

**详细文档：**
- [MCP 实现指南](docs/mcp-implementation-guide.md) - 完整的使用说明和对比
- [MCP 包 README](webclient-demo/src/main/java/com/example/webclient/springaimcp/README.md) - 快速开始

## 技术栈

- **Java 17**
- **Spring Boot 3.4.3**
- **Spring WebFlux** - 响应式 Web 框架
- **Reactor** - 响应式编程库
- **MyBatis / MyBatis-Plus** - 数据访问层
- **H2 Database** - 内存数据库（测试用）
- **Gradle 8.12.1** - 构建工具

## 快速开始

### 前置要求

- JDK 17 或更高版本
- Gradle 8.x（可选，项目包含 Gradle Wrapper）

### 克隆项目

```bash
git clone <repository-url>
cd webclient-demo-claude4.5/webclient-demo
```

### 构建项目

```bash
./gradlew build
```

### 运行应用

```bash
./gradlew bootRun
```

或者：

```bash
java -jar build/libs/webclient-demo-1.0.0.jar
```

应用将在 `http://localhost:8080` 启动。

### 运行测试

```bash
./gradlew test
```

## 项目结构

```
webclient-demo/
├── src/
│   ├── main/
│   │   ├── java/com/example/webclient/
│   │   │   ├── WebClientDemoApplication.java      # 主应用类
│   │   │   ├── client/                            # WebClient 客户端
│   │   │   │   ├── UserProfileClient.java         # 同步调用示例
│   │   │   │   ├── RecommendationClient.java      # 异步调用示例
│   │   │   │   ├── ContentStreamClient.java       # 流式调用示例
│   │   │   │   └── NotificationClient.java        # SSE 调用示例
│   │   │   ├── mcp/                               # MCP 简化实现
│   │   │   │   ├── McpMessage.java                # MCP 消息定义
│   │   │   │   ├── McpSseClient.java              # 简化 SSE 客户端
│   │   │   │   └── McpStreamClient.java           # 简化 Stream 客户端
│   │   │   ├── springaimcp/                       # ⭐ MCP 完整实现
│   │   │   │   ├── client/                        # MCP 客户端
│   │   │   │   │   ├── McpStdioClient.java        # STDIO 传输客户端
│   │   │   │   │   ├── McpSseTransportClient.java # SSE 传输客户端
│   │   │   │   │   └── McpStreamableHttpClient.java # HTTP 传输客户端
│   │   │   │   ├── server/                        # MCP 服务器
│   │   │   │   │   ├── McpStdioServer.java        # STDIO 传输服务器
│   │   │   │   │   ├── McpSseServer.java          # SSE 传输服务器
│   │   │   │   │   ├── McpStreamableHttpServer.java # HTTP 传输服务器
│   │   │   │   │   └── SseReceiverController.java # SSE 接收器示例
│   │   │   │   ├── example/                       # 使用示例
│   │   │   │   │   └── McpExampleController.java  # 完整示例控制器
│   │   │   │   └── README.md                      # MCP 快速开始
│   │   │   ├── config/                            # 配置类
│   │   │   │   ├── WebClientConfig.java           # WebClient 配置
│   │   │   │   └── JacksonConfig.java             # JSON 序列化配置
│   │   │   ├── filter/                            # 过滤器
│   │   │   │   ├── LoggingExchangeFilterFunction.java
│   │   │   │   ├── TracingExchangeFilterFunction.java
│   │   │   │   └── AuthenticationExchangeFilterFunction.java
│   │   │   ├── controller/                        # Mock Controllers
│   │   │   ├── entity/                            # 实体类
│   │   │   ├── dto/                               # 数据传输对象
│   │   │   └── exception/                         # 异常类
│   │   └── resources/
│   │       ├── application.yml                    # 应用配置
│   │       ├── schema.sql                         # 数据库表结构
│   │       └── data.sql                           # 测试数据
│   └── test/                                      # 测试代码
│       └── java/com/example/webclient/
│           └── client/                            # 客户端测试
├── docs/
│   ├── mcp-implementation-guide.md                # ⭐ MCP 实现指南
│   └── architecture.md                            # 架构文档
├── build.gradle                                   # Gradle 构建文件
└── README.md                                      # 本文件
```

## 核心功能

### 1. 同步调用（Blocking）

使用 `UserProfileClient` 演示同步调用：

```java
@Autowired
private UserProfileClient userProfileClient;

// 同步获取用户信息
User user = userProfileClient.getProfileSync("1");

// 同步创建用户
User newUser = new User();
newUser.setUsername("john");
User created = userProfileClient.createUserSync(newUser);
```

**特点：**
- 使用 `Mono.block()` 阻塞等待结果
- 适用于必须等待结果的业务场景
- 支持超时控制和重试策略

### 2. 异步调用（Reactive）

使用 `RecommendationClient` 演示异步调用：

```java
@Autowired
private RecommendationClient recommendationClient;

// 异步获取推荐列表
Mono<List<Recommendation>> recommendations = 
    recommendationClient.fetchAsync(request);

// 并发获取多个推荐
Flux<Recommendation> batch = 
    recommendationClient.getBatchAsync(List.of(1L, 2L, 3L));

// 组合多个请求
Mono<List<Recommendation>> combined = 
    recommendationClient.getCombinedRecommendations(userId);
```

**特点：**
- 返回 `Mono`/`Flux` 供响应式编排
- 支持并发请求和结果聚合
- 使用 `zip`、`flatMap` 等操作符组合请求

### 3. 响应式流式调用

使用 `ContentStreamClient` 演示流式数据处理：

```java
@Autowired
private ContentStreamClient contentStreamClient;

// 消费流式内容
Flux<StreamMessage> stream = 
    contentStreamClient.consumeContentFlux("topic");

// 带背压控制
Flux<StreamMessage> limited = 
    contentStreamClient.consumeWithBackpressure("topic", 10);

// 批量处理
Flux<List<StreamMessage>> batches = 
    contentStreamClient.consumeInBatches("topic", 50);
```

**特点：**
- 处理 NDJSON 格式的流式数据
- 支持背压控制，防止内存溢出
- 提供过滤、转换、批量处理等操作

### 4. SSE 调用

使用 `NotificationClient` 演示 Server-Sent Events：

```java
@Autowired
private NotificationClient notificationClient;

// 订阅通知
Flux<Notification> notifications = 
    notificationClient.subscribeNotifications("user123");

// 只订阅特定类型
Flux<Notification> alerts = 
    notificationClient.subscribeByType("user123", "alert");

// 设置超时
Flux<Notification> limited = 
    notificationClient.subscribeWithTimeout("user123", Duration.ofMinutes(5));
```

**特点：**
- 接收服务端推送的实时事件
- 支持自动重连机制
- 处理心跳和多种事件类型

### 5. MCP 调用

#### MCP 完整实现（推荐）

位于 `com.example.webclient.springaimcp` 包，支持三种官方传输方式。

##### Streamable HTTP 方式

```java
@Autowired
private McpStreamableHttpClient mcpClient;

// 调用工具
Map<String, Object> args = Map.of("query", "MCP examples");
mcpClient.callTool("/api/springai/mcp/stream", "search", args)
    .subscribe(result -> log.info("Result: {}", result));

// 流式调用（获取进度）
mcpClient.callToolStreaming("/api/springai/mcp/stream", "search", args)
    .subscribe(chunk -> log.info("Chunk: {}", chunk));

// 长任务（带进度回调）
mcpClient.sendWithProgress(
    "/api/springai/mcp/stream",
    "long_task",
    Map.of("steps", 10),
    progress -> log.info("Progress: {}", progress.getResult())
).subscribe();
```

##### SSE 方式

```java
@Autowired
private McpSseTransportClient mcpSseClient;

// 建立连接并握手
Map<String, Object> info = mcpSseClient.connect("/api/springai/mcp/sse").block();
String endpoint = (String) info.get("endpoint");

// 调用工具
mcpSseClient.callTool(endpoint, "get_weather", Map.of("city", "Beijing"))
    .subscribe(result -> log.info("Weather: {}", result));

// 订阅通知
mcpSseClient.subscribeNotifications()
    .subscribe(notification -> 
        log.info("Notification: {}", notification.getMethod())
    );
```

##### STDIO 方式

```java
@Autowired
private McpStdioClient mcpStdioClient;

// 启动 MCP 服务器进程
mcpStdioClient.connect("node mcp-server.js").block();

// 调用工具
mcpStdioClient.callTool("echo", Map.of("text", "Hello!"))
    .subscribe(result -> log.info("Echo: {}", result));

// 读取资源
mcpStdioClient.readResource("file:///data/file.txt")
    .subscribe(content -> log.info("Content: {}", content));
```

#### MCP 简化实现（原有）

位于 `com.example.webclient.mcp` 包，仅支持 SSE 和 HTTP Stream。

#### MCP SSE 方式

```java
@Autowired
private McpSseClient mcpSseClient;

// 建立 MCP SSE 连接
Flux<McpMessage> messages = mcpSseClient.connect("/api/mcp/sse");

// 发送请求并等待响应
Mono<McpMessage> response = 
    mcpSseClient.sendRequest("/api/mcp/sse", "execute", params);
```

#### MCP Stream 方式

```java
@Autowired
private McpStreamClient mcpStreamClient;

// 发送流式请求
Flux<McpMessage> stream = 
    mcpStreamClient.sendStreamRequest("/api/mcp/stream", "process", params);

// 带进度回调
Mono<McpMessage> result = mcpStreamClient.sendWithProgress(
    "/api/mcp/stream", 
    "longTask", 
    params,
    progress -> log.info("Progress: {}", progress)
);
```

## 配置说明

### WebClient 配置

在 `application.yml` 中配置：

```yaml
webclient:
  base-url: http://localhost:8080
  connection-timeout: 5000
  read-timeout: 30000
  write-timeout: 30000
  max-connections: 500
  max-idle-time: 20
  max-memory-size: 16777216
```

### 认证配置

支持多种认证方式：

```yaml
webclient:
  auth:
    type: bearer  # none, bearer, apikey, basic
    token: your-token
    api-key: your-api-key
    api-key-header: X-API-Key
    username: user
    password: pass
```

### 环境配置

项目支持多环境配置（dev、test、prod），通过 Spring Profile 切换：

```bash
# 开发环境
java -jar app.jar --spring.profiles.active=dev

# 测试环境
java -jar app.jar --spring.profiles.active=test

# 生产环境
java -jar app.jar --spring.profiles.active=prod
```

## Mock API 端点

应用内置了 Mock Controller，可用于测试：

### 基础 API
- **用户 API**：`/api/users`
- **推荐 API**：`/api/recommendations`
- **流式 API**：`/api/stream/content`
- **SSE API**：`/api/notifications/subscribe`

### MCP 简化实现 API
- **MCP SSE API**：`/api/mcp/sse`
- **MCP Stream API**：`/api/mcp/stream`

### MCP 完整实现 API (⭐ 推荐)

#### 服务端点
- **STDIO 服务器**：程序化启动（见文档）
- **SSE 服务器**：
  - 连接：`GET /api/springai/mcp/sse` (需要 `X-Session-Id` 头)
  - 发送请求：`POST /api/springai/mcp/sse/message`
- **Streamable HTTP 服务器**：`POST /api/springai/mcp/stream`

#### 示例端点
- **健康检查**：`GET /api/examples/mcp/health`
- **工具调用（流式）**：`POST /api/examples/mcp/stream/tool/call`
- **工具调用（带进度）**：`POST /api/examples/mcp/stream/tool/call-with-progress`
- **长任务**：`POST /api/examples/mcp/stream/long-task?steps=10`
- **提示词管理**：`GET /api/examples/mcp/prompts/comprehensive`
- **资源读取**：`GET /api/examples/mcp/stream/resource?uri=file:///data/file.txt`
- **批量工具调用**：`POST /api/examples/mcp/stream/tools/batch`

#### SSE 接收器端点
- **接收 SSE 流**：`POST /api/springai/sse/receive`
- **转发 SSE 流**：`POST /api/springai/sse/forward`
- **聚合 SSE 源**：`POST /api/springai/sse/aggregate/{sourceId}`
- **获取聚合流**：`GET /api/springai/sse/aggregated`

## 监控与运维

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

### Metrics

```bash
curl http://localhost:8080/actuator/metrics
```

### H2 Console

访问 `http://localhost:8080/h2-console` 查看数据库。

**连接信息：**
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: （留空）

## 测试

### 单元测试

```bash
./gradlew test
```

### 集成测试

```bash
./gradlew integrationTest
```

### 测试覆盖率

```bash
./gradlew jacocoTestReport
```

报告位于 `build/reports/jacoco/test/html/index.html`

## 最佳实践

### 1. 错误处理

所有客户端都实现了统一的错误处理：

- 4xx/5xx 错误转换为领域异常
- 超时处理
- 重试策略（针对幂等请求）

### 2. 日志追踪

所有请求自动添加 `traceId` 和 `spanId`，便于分布式追踪。

### 3. 连接池管理

配置了合理的连接池参数：

- 最大连接数：500
- 最大空闲时间：20 秒
- 连接超时：5 秒

### 4. 背压控制

流式调用使用 `limitRate` 控制背压，防止内存溢出。

## 扩展建议

- 集成 Resilience4j 实现熔断和限流
- 接入 Spring Cloud Sleuth 实现分布式追踪
- 集成 OAuth2 Client Credentials 认证
- 添加 gRPC / GraphQL 支持
- 构建统一的 API Gateway

## 参考文档

- [Spring WebFlux 官方文档](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html)
- [Reactor 官方文档](https://projectreactor.io/docs/core/release/reference/)
- [WebClient 官方指南](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-client)

## 许可证

MIT License

## 作者

AI Agent

## 更新日志

### v1.0.0 (2025-10-12)
- 初始版本发布
- 实现同步、异步、流式、SSE、MCP 调用
- 提供完整的测试用例
- 添加 Mock API 支持