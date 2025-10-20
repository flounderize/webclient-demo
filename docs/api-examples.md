# WebClient API 使用示例

## 概述

本文档提供 WebClient 各种调用方式的详细使用示例和最佳实践。

## 目录

1. [同步调用](#1-同步调用)
2. [异步调用](#2-异步调用)
3. [流式调用](#3-流式调用)
4. [SSE 调用](#4-sse-调用)
5. [MCP 调用](#5-mcp-调用)
6. [高级用法](#6-高级用法)
7. [最佳实践](#7-最佳实践)

---

## 1. 同步调用

### 1.1 基本用法

```java
@Autowired
private UserProfileClient userProfileClient;

// 获取用户信息
public void getUserExample() {
    try {
        User user = userProfileClient.getProfileSync("123");
        System.out.println("User: " + user.getUsername());
    } catch (WebClientException e) {
        log.error("Failed to get user", e);
    }
}
```

### 1.2 CRUD 操作

#### 创建（Create）

```java
public User createUser() {
    User newUser = new User();
    newUser.setUsername("john_doe");
    newUser.setEmail("john@example.com");
    newUser.setAge(25);
    
    return userProfileClient.createUserSync(newUser);
}
```

#### 读取（Read）

```java
public User getUser(String userId) {
    return userProfileClient.getProfileSync(userId);
}
```

#### 更新（Update）

```java
public User updateUser(String userId) {
    User updatedUser = new User();
    updatedUser.setUsername("john_updated");
    updatedUser.setEmail("john.new@example.com");
    
    return userProfileClient.updateUserSync(userId, updatedUser);
}
```

#### 删除（Delete）

```java
public void deleteUser(String userId) {
    userProfileClient.deleteUserSync(userId);
    log.info("User deleted: {}", userId);
}
```

### 1.3 错误处理

```java
public User getUserWithErrorHandling(String userId) {
    try {
        return userProfileClient.getProfileSync(userId);
    } catch (WebClientException e) {
        if (e.getStatusCode() != null && e.getStatusCode() == 404) {
            log.warn("User not found: {}", userId);
            return null;
        } else {
            log.error("Error fetching user: {}", userId, e);
            throw e;
        }
    }
}
```

### 1.4 适用场景

✅ **适合**：
- 简单的 CRUD 操作
- 必须等待结果的业务流程
- 集成到非响应式代码中

❌ **不适合**：
- 高并发场景
- 需要组合多个请求
- 响应式应用

---

## 2. 异步调用

### 2.1 基本用法

```java
@Autowired
private RecommendationClient recommendationClient;

// 异步获取推荐
public Mono<List<Recommendation>> getRecommendations(Long userId) {
    RecommendationRequest request = new RecommendationRequest(userId, "personal", 10);
    return recommendationClient.fetchAsync(request);
}
```

### 2.2 订阅结果

```java
public void subscribeRecommendations(Long userId) {
    getRecommendations(userId)
        .subscribe(
            recommendations -> {
                log.info("Received {} recommendations", recommendations.size());
                recommendations.forEach(rec -> log.info("Rec: {}", rec.getTitle()));
            },
            error -> log.error("Error fetching recommendations", error),
            () -> log.info("Completed")
        );
}
```

### 2.3 阻塞获取结果

```java
public List<Recommendation> getRecommendationsBlocking(Long userId) {
    return getRecommendations(userId)
        .block(Duration.ofSeconds(10)); // 最多等待 10 秒
}
```

### 2.4 并发请求

#### 并发执行，收集所有结果

```java
public Mono<List<Recommendation>> getBatchRecommendations() {
    List<Long> userIds = List.of(1L, 2L, 3L, 4L, 5L);
    
    List<Mono<List<Recommendation>>> monos = userIds.stream()
        .map(this::getRecommendations)
        .toList();
    
    return Flux.merge(monos)
        .collectList()
        .map(listOfLists -> listOfLists.stream()
            .flatMap(List::stream)
            .collect(Collectors.toList()));
}
```

#### 并发执行，控制并发数

```java
public Flux<Recommendation> getBatchRecommendationsFlux(List<Long> userIds) {
    return Flux.fromIterable(userIds)
        .flatMap(userId -> {
            RecommendationRequest request = new RecommendationRequest(userId, "personal", 10);
            return recommendationClient.fetchAsync(request);
        }, 5) // 最多 5 个并发请求
        .flatMapIterable(list -> list); // 展开列表
}
```

### 2.5 组合请求

#### Zip - 等待所有请求完成

```java
public Mono<CombinedResult> getCombinedData(Long userId) {
    Mono<User> userMono = userProfileClient.getByIdAsync(userId);
    Mono<List<Recommendation>> recsMono = getRecommendations(userId);
    
    return Mono.zip(userMono, recsMono)
        .map(tuple -> new CombinedResult(tuple.getT1(), tuple.getT2()));
}
```

#### FlatMap - 串行执行

```java
public Mono<Recommendation> getFirstRecommendationDetail(Long userId) {
    RecommendationRequest request = new RecommendationRequest(userId, "personal", 5);
    
    return recommendationClient.fetchAsync(request)
        .flatMap(recommendations -> {
            if (recommendations.isEmpty()) {
                return Mono.empty();
            }
            Long firstId = recommendations.get(0).getId();
            return recommendationClient.getByIdAsync(firstId);
        });
}
```

#### Merge - 合并多个流

```java
public Flux<Recommendation> getMergedRecommendations(Long userId) {
    Mono<List<Recommendation>> personal = 
        recommendationClient.fetchAsync(new RecommendationRequest(userId, "personal", 5));
    Mono<List<Recommendation>> popular = 
        recommendationClient.fetchAsync(new RecommendationRequest(null, "popular", 5));
    
    return Flux.merge(
        personal.flatMapIterable(list -> list),
        popular.flatMapIterable(list -> list)
    );
}
```

### 2.6 错误处理

#### onErrorReturn - 错误时返回默认值

```java
public Mono<List<Recommendation>> getRecommendationsWithDefault(Long userId) {
    return getRecommendations(userId)
        .onErrorReturn(List.of()); // 错误时返回空列表
}
```

#### onErrorResume - 错误时执行备用逻辑

```java
public Mono<List<Recommendation>> getRecommendationsWithFallback(Long userId) {
    return getRecommendations(userId)
        .onErrorResume(error -> {
            log.warn("Primary source failed, using fallback", error);
            return getFallbackRecommendations(userId);
        });
}
```

#### doOnError - 错误时执行副作用

```java
public Mono<List<Recommendation>> getRecommendationsWithLogging(Long userId) {
    return getRecommendations(userId)
        .doOnError(error -> log.error("Failed to get recommendations for user {}", userId, error));
}
```

### 2.7 超时处理

```java
public Mono<List<Recommendation>> getRecommendationsWithTimeout(Long userId) {
    return getRecommendations(userId)
        .timeout(Duration.ofSeconds(5))
        .onErrorResume(TimeoutException.class, e -> {
            log.warn("Request timeout for user {}", userId);
            return Mono.just(List.of());
        });
}
```

---

## 3. 流式调用

### 3.1 基本用法

```java
@Autowired
private ContentStreamClient contentStreamClient;

// 消费流式内容
public void consumeStream() {
    contentStreamClient.consumeContentFlux("tech")
        .subscribe(
            message -> log.info("Received: {}", message.getContent()),
            error -> log.error("Stream error", error),
            () -> log.info("Stream completed")
        );
}
```

### 3.2 过滤和转换

```java
public Flux<String> getFilteredContent(String topic, String keyword) {
    return contentStreamClient.consumeContentFlux(topic)
        .filter(msg -> msg.getContent().contains(keyword))
        .map(StreamMessage::getContent)
        .map(String::toUpperCase);
}
```

### 3.3 批量处理

```java
public Flux<List<StreamMessage>> processBatches(String topic) {
    return contentStreamClient.consumeContentFlux(topic)
        .buffer(10) // 每 10 条打包一次
        .doOnNext(batch -> log.info("Processing batch of {}", batch.size()))
        .map(this::processBatch);
}

private List<StreamMessage> processBatch(List<StreamMessage> batch) {
    // 批量处理逻辑
    return batch;
}
```

### 3.4 背压控制

```java
public void consumeWithBackpressure(String topic) {
    contentStreamClient.consumeContentFlux(topic)
        .limitRate(5) // 每次最多请求 5 条
        .doOnRequest(n -> log.info("Requested {} items", n))
        .subscribe(message -> {
            // 慢速处理
            processSlowly(message);
        });
}
```

### 3.5 窗口化处理

```java
public Flux<Long> countByTimeWindow(String topic) {
    return contentStreamClient.consumeContentFlux(topic)
        .window(Duration.ofSeconds(10)) // 10 秒一个窗口
        .flatMap(window -> window.count())
        .doOnNext(count -> log.info("Received {} messages in window", count));
}
```

---

## 4. SSE 调用

### 4.1 基本订阅

```java
@Autowired
private NotificationClient notificationClient;

// 订阅通知
public void subscribeNotifications(String userId) {
    notificationClient.subscribeNotifications(userId)
        .subscribe(
            notification -> handleNotification(notification),
            error -> log.error("SSE error", error),
            () -> log.info("SSE stream ended")
        );
}

private void handleNotification(Notification notification) {
    log.info("Notification: {} - {}", notification.getTitle(), notification.getMessage());
    // 处理通知逻辑
}
```

### 4.2 过滤特定类型

```java
public void subscribeAlerts(String userId) {
    notificationClient.subscribeNotifications(userId)
        .filter(notif -> "alert".equals(notif.getType()))
        .subscribe(alert -> handleAlert(alert));
}
```

### 4.3 限制数量

```java
public Mono<List<Notification>> getFirstTenNotifications(String userId) {
    return notificationClient.subscribeNotifications(userId)
        .take(10)
        .collectList();
}
```

### 4.4 超时处理

```java
public void subscribeWithTimeout(String userId) {
    notificationClient.subscribeNotifications(userId)
        .timeout(Duration.ofMinutes(5))
        .doOnError(TimeoutException.class, e -> log.warn("SSE timeout"))
        .retry(3) // 超时后重试 3 次
        .subscribe(this::handleNotification);
}
```

### 4.5 取消订阅

```java
public Disposable subscribeNotifications(String userId) {
    Disposable subscription = notificationClient.subscribeNotifications(userId)
        .subscribe(this::handleNotification);
    
    // 稍后取消订阅
    // subscription.dispose();
    
    return subscription;
}
```

---

## 5. MCP 调用

### 5.1 MCP SSE

#### 建立连接

```java
@Autowired
private McpSseClient mcpSseClient;

public void connectMcp() {
    mcpSseClient.connect("/api/mcp/sse")
        .subscribe(
            message -> handleMcpMessage(message),
            error -> log.error("MCP error", error)
        );
}
```

#### 发送请求

```java
public Mono<McpMessage> executeMcpCommand(String command, Map<String, Object> params) {
    return mcpSseClient.sendRequest("/api/mcp/sse", command, params)
        .doOnSuccess(response -> {
            if (response.getError() != null) {
                log.error("MCP error: {}", response.getError());
            } else {
                log.info("MCP result: {}", response.getResult());
            }
        });
}
```

#### 订阅通知

```java
public void subscribeMcpNotifications() {
    mcpSseClient.subscribeNotifications("/api/mcp/sse")
        .subscribe(notification -> {
            log.info("MCP notification: method={}, params={}", 
                notification.getMethod(), notification.getParams());
        });
}
```

### 5.2 MCP Stream

#### 发送流式请求

```java
@Autowired
private McpStreamClient mcpStreamClient;

public void executeLongRunningTask(Map<String, Object> params) {
    mcpStreamClient.sendStreamRequest("/api/mcp/stream", "longTask", params)
        .subscribe(
            message -> {
                // 处理进度更新
                if (message.getResult() != null) {
                    log.info("Progress: {}", message.getResult());
                }
            },
            error -> log.error("Task failed", error),
            () -> log.info("Task completed")
        );
}
```

#### 带进度回调

```java
public Mono<McpMessage> executeWithProgress(Map<String, Object> params) {
    return mcpStreamClient.sendWithProgress(
        "/api/mcp/stream",
        "process",
        params,
        progress -> {
            // 更新进度 UI
            updateProgressBar(progress);
        }
    );
}
```

---

## 6. 高级用法

### 6.1 自定义 WebClient

```java
@Bean
public WebClient customWebClient(WebClient.Builder builder) {
    return builder
        .baseUrl("https://custom-api.example.com")
        .defaultHeader("X-Custom-Header", "value")
        .filter((request, next) -> {
            log.info("Custom filter: {}", request.url());
            return next.exchange(request);
        })
        .build();
}
```

### 6.2 动态 URL

```java
public Mono<String> dynamicUrl(String endpoint, Map<String, String> params) {
    return webClient.get()
        .uri(uriBuilder -> {
            UriBuilder builder = uriBuilder.path(endpoint);
            params.forEach(builder::queryParam);
            return builder.build();
        })
        .retrieve()
        .bodyToMono(String.class);
}
```

### 6.3 自定义请求头

```java
public Mono<Response> withCustomHeaders() {
    return webClient.get()
        .uri("/api/resource")
        .header("X-Request-Id", UUID.randomUUID().toString())
        .header("X-Client-Version", "1.0.0")
        .headers(headers -> {
            headers.set("X-Custom", "value");
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        })
        .retrieve()
        .bodyToMono(Response.class);
}
```

### 6.4 表单提交

```java
public Mono<Response> submitForm(Map<String, String> formData) {
    return webClient.post()
        .uri("/api/form")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .bodyValue(formData)
        .retrieve()
        .bodyToMono(Response.class);
}
```

### 6.5 文件上传

```java
public Mono<Response> uploadFile(Path filePath) {
    MultipartBodyBuilder builder = new MultipartBodyBuilder();
    builder.part("file", new FileSystemResource(filePath));
    builder.part("description", "File description");
    
    return webClient.post()
        .uri("/api/upload")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .bodyValue(builder.build())
        .retrieve()
        .bodyToMono(Response.class);
}
```

---

## 7. 最佳实践

### 7.1 错误处理

```java
public Mono<Result> robustApiCall() {
    return webClient.get()
        .uri("/api/resource")
        .retrieve()
        .bodyToMono(Result.class)
        // 超时
        .timeout(Duration.ofSeconds(10))
        // 重试（只重试幂等请求）
        .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
        // 错误时记录日志
        .doOnError(e -> log.error("API call failed", e))
        // 错误时返回默认值
        .onErrorReturn(new Result());
}
```

### 7.2 资源管理

```java
public void properResourceManagement() {
    Disposable subscription = contentStreamClient.consumeContentFlux("topic")
        .subscribe(message -> process(message));
    
    // 注册关闭钩子
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        if (!subscription.isDisposed()) {
            subscription.dispose();
            log.info("Disposed subscription");
        }
    }));
}
```

### 7.3 日志追踪

```java
public Mono<Result> withTracing() {
    String traceId = MDC.get("traceId");
    
    return webClient.get()
        .uri("/api/resource")
        .header("X-Trace-Id", traceId)
        .retrieve()
        .bodyToMono(Result.class)
        .doOnSubscribe(s -> log.info("Request started, traceId: {}", traceId))
        .doOnSuccess(r -> log.info("Request succeeded, traceId: {}", traceId))
        .doOnError(e -> log.error("Request failed, traceId: {}", traceId, e));
}
```

### 7.4 缓存

```java
private final Map<String, Mono<Result>> cache = new ConcurrentHashMap<>();

public Mono<Result> cachedApiCall(String key) {
    return cache.computeIfAbsent(key, k -> 
        webClient.get()
            .uri("/api/resource/{id}", k)
            .retrieve()
            .bodyToMono(Result.class)
            .cache(Duration.ofMinutes(5)) // 缓存 5 分钟
    );
}
```

### 7.5 熔断

```java
@CircuitBreaker(name = "backend", fallbackMethod = "fallback")
public Mono<Result> callWithCircuitBreaker() {
    return webClient.get()
        .uri("/api/resource")
        .retrieve()
        .bodyToMono(Result.class);
}

public Mono<Result> fallback(Exception e) {
    log.warn("Circuit breaker activated", e);
    return Mono.just(new Result("fallback"));
}
```

---

## 总结

本文档涵盖了 WebClient 的主要使用场景和最佳实践。在实际使用中：

- **同步调用**适合简单场景
- **异步调用**适合高并发和复杂编排
- **流式调用**适合大量数据处理
- **SSE** 适合实时推送
- **MCP** 适合 AI Agent 场景

始终记住：
1. 合理处理错误
2. 设置超时
3. 控制并发
4. 注意资源释放
5. 添加日志追踪
