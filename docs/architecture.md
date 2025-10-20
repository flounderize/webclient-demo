# WebClient 示例工程架构文档

## 1. 系统概述

WebClient Demo 是一个面向企业级 AI Agent 的 Spring Boot 示例工程，展示了如何使用 Spring WebFlux 的 WebClient 进行各种类型的 HTTP 调用。

### 1.1 设计目标

- 提供生产级别的 WebClient 使用模板
- 演示同步、异步、流式、SSE、MCP 等多种调用方式
- 统一错误处理、日志追踪、认证鉴权等横切关注点
- 作为现有 Spring Boot 应用集成 WebClient 的参考实现

### 1.2 核心特性

- **多种调用模式**：同步、异步、流式、SSE、MCP
- **统一配置管理**：连接池、超时、重试策略可配置
- **过滤器链**：日志、追踪、认证过滤器
- **错误处理**：统一异常处理和重试机制
- **测试友好**：完整的单元测试和集成测试

## 2. 架构设计

### 2.1 分层架构

```
┌─────────────────────────────────────────────┐
│              Application Layer              │
│  (Controllers, Services, Scheduled Tasks)   │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│              Client Layer                   │
│  (WebClient Wrappers)                       │
│  - UserProfileClient                        │
│  - RecommendationClient                     │
│  - ContentStreamClient                      │
│  - NotificationClient                       │
│  - McpSseClient / McpStreamClient           │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│           WebClient Core                    │
│  (Configuration, Filters, Strategies)       │
└─────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────┐
│           Reactor Netty                     │
│  (HTTP Client, Connection Pool)             │
└─────────────────────────────────────────────┘
```

### 2.2 组件职责

#### 2.2.1 Configuration Layer

- **WebClientConfig**: WebClient 实例配置和创建
- **JacksonConfig**: JSON 序列化/反序列化配置
- **连接池管理**: 配置 Reactor Netty 连接池参数

#### 2.2.2 Filter Layer

- **LoggingExchangeFilterFunction**: 请求/响应日志记录
- **TracingExchangeFilterFunction**: 分布式追踪（traceId/spanId）
- **AuthenticationExchangeFilterFunction**: 认证信息注入

#### 2.2.3 Client Layer

各个客户端类封装了对不同服务的调用逻辑：

- **UserProfileClient**: 同步调用示例
- **RecommendationClient**: 异步调用示例
- **ContentStreamClient**: 流式调用示例
- **NotificationClient**: SSE 调用示例
- **McpSseClient / McpStreamClient**: MCP 调用示例

#### 2.2.4 Mock Layer

提供测试用的 Mock API：

- **UserMockController**: 用户 CRUD API
- **RecommendationMockController**: 推荐 API
- **StreamMockController**: 流式数据 API
- **NotificationMockController**: SSE API
- **McpMockController**: MCP API

## 3. 调用模式详解

### 3.1 同步调用（Blocking）

```
Client Code
    ↓
UserProfileClient.getProfileSync()
    ↓
WebClient.get().retrieve().bodyToMono()
    ↓
Mono.block()  ← 阻塞等待
    ↓
Return User
```

**适用场景：**
- 必须等待结果才能继续的业务流程
- 简单的 CRUD 操作
- 集成到传统的非响应式代码中

**注意事项：**
- 不应在响应式流中使用 block()
- 需要设置合理的超时时间
- 考虑线程阻塞对性能的影响

### 3.2 异步调用（Reactive）

```
Client Code
    ↓
RecommendationClient.fetchAsync()
    ↓
WebClient.post().retrieve().bodyToMono()
    ↓
Return Mono<List<Recommendation>>
    ↓
[上层可使用 flatMap/zipWith/etc 继续编排]
```

**适用场景：**
- 需要组合多个异步调用
- 高并发场景
- 响应式应用

**操作符示例：**
- `flatMap`: 串行组合请求
- `zip`: 并行等待多个请求完成
- `merge`: 合并多个流
- `switchMap`: 切换到新的流

### 3.3 流式调用（Streaming）

```
Client Code
    ↓
ContentStreamClient.consumeContentFlux()
    ↓
WebClient.get().accept(NDJSON).retrieve().bodyToFlux()
    ↓
Return Flux<StreamMessage>
    ↓
[流式处理: filter/map/buffer/window]
```

**适用场景：**
- 大量数据流
- 实时数据处理
- 日志流、监控数据流

**背压控制：**
- `limitRate(n)`: 限制请求速率
- `buffer()`: 批量缓冲
- `onBackpressureDrop()`: 背压时丢弃数据

### 3.4 SSE 调用

```
Client Code
    ↓
NotificationClient.subscribeNotifications()
    ↓
WebClient.get().accept(TEXT_EVENT_STREAM).retrieve()
    ↓
bodyToFlux(ServerSentEvent.class)
    ↓
Return Flux<Notification>
    ↓
[自动重连、心跳处理]
```

**适用场景：**
- 实时通知推送
- 服务端状态更新
- 聊天消息流

**特性：**
- 单向通信（服务端到客户端）
- 自动重连机制
- 事件类型区分

### 3.5 MCP 调用

#### MCP SSE 方式

```
Client Code
    ↓
McpSseClient.sendRequest()
    ↓
SSE Connection (bidirectional semantics)
    ↓
Filter by request ID
    ↓
Return Mono<McpMessage>
```

#### MCP Streamable HTTP 方式

```
Client Code
    ↓
McpStreamClient.sendStreamRequest()
    ↓
POST with NDJSON response
    ↓
Flux<McpMessage> (progress + final result)
    ↓
Return final or collect all
```

**适用场景：**
- AI Agent 与 Model Context Protocol 服务通信
- 长时间运行的任务（带进度反馈）
- 复杂的双向通信场景

## 4. 配置管理

### 4.1 WebClient 配置

```yaml
webclient:
  base-url: http://localhost:8080
  connection-timeout: 5000      # 连接超时（毫秒）
  read-timeout: 30000           # 读取超时（毫秒）
  write-timeout: 30000          # 写入超时（毫秒）
  max-connections: 500          # 最大连接数
  max-idle-time: 20             # 最大空闲时间（秒）
  max-memory-size: 16777216     # 最大内存（字节，16MB）
```

### 4.2 连接池配置

使用 Reactor Netty ConnectionProvider：

- **maxConnections**: 连接池最大连接数
- **maxIdleTime**: 连接最大空闲时间
- **maxLifeTime**: 连接最大生命周期
- **pendingAcquireTimeout**: 获取连接超时时间
- **evictInBackground**: 后台清理间隔

### 4.3 认证配置

支持三种认证方式：

1. **Bearer Token**:
   ```yaml
   auth:
     type: bearer
     token: your-jwt-token
   ```

2. **API Key**:
   ```yaml
   auth:
     type: apikey
     api-key: your-api-key
     api-key-header: X-API-Key
   ```

3. **Basic Auth**:
   ```yaml
   auth:
     type: basic
     username: user
     password: pass
   ```

## 5. 错误处理策略

### 5.1 HTTP 状态码处理

```java
.onStatus(
    status -> status.is4xxClientError() || status.is5xxServerError(),
    clientResponse -> clientResponse.bodyToMono(String.class)
        .flatMap(errorBody -> Mono.error(new WebClientException(errorBody)))
)
```

### 5.2 重试策略

```java
.retry(2)  // 简单重试
// 或
.retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2)))  // 固定延迟重试
// 或
.retryWhen(Retry.backoff(5, Duration.ofSeconds(1)))  // 指数退避重试
```

### 5.3 超时处理

```java
.timeout(Duration.ofSeconds(30))
```

### 5.4 降级处理

```java
.onErrorReturn(defaultValue)
// 或
.onErrorResume(e -> Mono.just(fallbackValue))
```

## 6. 日志与追踪

### 6.1 请求日志

LoggingExchangeFilterFunction 记录：
- 请求方法和 URL
- 请求头
- 响应状态码
- 响应头
- 请求耗时

### 6.2 分布式追踪

TracingExchangeFilterFunction 实现：
- 生成或传递 traceId
- 生成 spanId
- 注入追踪头（X-Trace-Id, X-Span-Id）
- 在 MDC 中设置追踪信息

### 6.3 日志级别

```yaml
logging:
  level:
    com.example.webclient: DEBUG
    org.springframework.web.reactive.function.client: DEBUG
```

## 7. 性能优化

### 7.1 连接复用

- 使用连接池避免频繁创建连接
- 配置合理的连接保活时间
- 定期清理空闲连接

### 7.2 背压控制

- 使用 `limitRate()` 控制消费速率
- 使用 `buffer()` 批量处理
- 避免内存溢出

### 7.3 并发控制

```java
.flatMap(item -> process(item), concurrency)  // 控制并发数
```

### 7.4 内存管理

- 配置 `maxInMemorySize` 限制内存使用
- 对大文件使用流式传输
- 及时释放资源

## 8. 测试策略

### 8.1 单元测试

使用 StepVerifier 测试响应式流：

```java
StepVerifier.create(flux)
    .expectNext(item1)
    .expectNext(item2)
    .verifyComplete();
```

### 8.2 集成测试

使用 @SpringBootTest 启动应用：

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
```

### 8.3 Mock 服务

内置 Mock Controller 提供测试 API。

### 8.4 测试覆盖

- 正常场景
- 错误场景（404, 500）
- 超时场景
- 重试场景
- 并发场景

## 9. 部署与运维

### 9.1 健康检查

使用 Spring Boot Actuator：

```
GET /actuator/health
```

### 9.2 监控指标

```
GET /actuator/metrics
GET /actuator/prometheus
```

### 9.3 配置外部化

使用环境变量覆盖配置：

```bash
export WEBCLIENT_BASE_URL=http://prod-api.example.com
export WEBCLIENT_AUTH_TOKEN=prod-token
```

### 9.4 优雅关闭

```yaml
server:
  shutdown: graceful
```

## 10. 扩展建议

### 10.1 熔断器

集成 Resilience4j CircuitBreaker：

```java
@CircuitBreaker(name = "backend", fallbackMethod = "fallback")
public Mono<Response> call() {
    // ...
}
```

### 10.2 限流

使用 Resilience4j RateLimiter：

```java
@RateLimiter(name = "backend")
public Mono<Response> call() {
    // ...
}
```

### 10.3 缓存

集成 Spring Cache 或 Caffeine。

### 10.4 链路追踪

集成 Spring Cloud Sleuth + Zipkin。

## 11. 安全建议

- 敏感配置使用环境变量或密钥管理服务
- 启用 HTTPS
- 实现 Token 刷新机制
- 限制 API 调用频率
- 日志脱敏

## 12. 参考资料

- [Spring WebFlux Reference](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html)
- [Reactor Reference Guide](https://projectreactor.io/docs/core/release/reference/)
- [WebClient API Documentation](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/reactive/function/client/WebClient.html)
- [Reactor Netty Reference](https://projectreactor.io/docs/netty/release/reference/)
- [Model Context Protocol Specification](https://spec.modelcontextprotocol.io/)
