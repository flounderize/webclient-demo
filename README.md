# WebClient Demo - Spring Boot WebClient 示例工程

## 项目简介

这是一个面向企业级 AI Agent 的 Spring Boot WebClient 示例工程，演示了 WebClient 在不同场景下的使用方式，包括：

- **同步调用** - 使用 `Mono.block()` 阻塞获取结果
- **异步调用** - 返回 `Mono`/`Flux` 供响应式编排
- **响应式流式调用** - 处理流式数据（NDJSON）
- **SSE 调用** - 订阅 Server-Sent Events
- **MCP 调用** - 支持 Model Context Protocol（SSE + Streamable HTTP）

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
│   │   │   ├── mcp/                               # MCP 相关
│   │   │   │   ├── McpMessage.java                # MCP 消息定义
│   │   │   │   ├── McpSseClient.java              # MCP SSE 客户端
│   │   │   │   └── McpStreamClient.java           # MCP Stream 客户端
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

- **用户 API**：`/api/users`
- **推荐 API**：`/api/recommendations`
- **流式 API**：`/api/stream/content`
- **SSE API**：`/api/notifications/subscribe`
- **MCP SSE API**：`/api/mcp/sse`
- **MCP Stream API**：`/api/mcp/stream`

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