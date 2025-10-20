目标：我需要一个WebClient使用示例工程，为现有Spring Boot工程提供参考甚至基础组建以便于将现在的HTTP客户端外调、SSE调用、MCP调用等统一为使用WebClient，以便提升项目外调功能能力、降低外调构建难度。

先梳理需求要点 → 定义工程定位和总体目标 → 描述技术栈与架构 → 逐项说明各类WebClient 调用能力及对应测试要求 → 补充数据建模、配置、安全与运维要点 → 罗列交付物与后续扩展建议。

使用中文生成所有文档、注释、回复！

## 功能概览

- **项目定位**：面向企业级 AI Agent 的 Spring Boot WebClient 示例工程，作为现有 Spring Boot 应用集成 WebClient 调用能力的蓝本。
- **主要目标**：
  - 演示同步、异步、响应式流式、Server-Sent Events (SSE) 以及 Model Context Protocol (MCP)（SSE + Streamable HTTP）调用方式。
  - 提供基于 Java 对象与 JSON/YAML 等企业常见格式的请求构造与响应映射模板。
  - 附带覆盖每种调用方式的测试用例，便于回归与持续集成。
- **AI Agent 使用场景**：
  - 统一封装 HTTP 访问层，便于在智能代理中编排对外服务调用。
  - 提供可复用的策略/拦截器，实现跨服务的鉴权、幂等与观测。

## 技术栈与系统架构

- **语言与平台**：Java 17 + Spring Boot 3.4.3。
- **构建工具**：Gradle 8.12.1，采用 Groovy DSL（`build.gradle`）。
- **依赖管理**：
  - Spring Boot BOM 统一版本。
  - 核心依赖：`spring-boot-starter-webflux`、`spring-boot-starter-validation`、`spring-boot-starter-actuator`。
  - 数据访问：`mybatis-spring-boot-starter`、`mybatis-plus-boot-starter`。
  - 测试：`spring-boot-starter-test`（JUnit 5）、`reactor-test`、`wiremock-jre8`（或 MockWebServer）模拟 HTTP。
- **功能建议**，仅供参考：
  - WebClient 配置、过滤器、序列化、错误处理。
  - 业务调用示例（同步/异步/流式/SSE/MCP）。
  - 公共测试基类、模拟服务、契约定义。
  - 日志追踪、幂等、熔断、重试策略（与 Resilience4j 整合可选）。
- **模块建议**：
  - 在同一个Gradle模块中实现。模块名称webclient-demo

## WebClient 功能设计

### 配置与基础设施

- **WebClient Builder**：
  - 通过 `WebClient.Builder` 注入 `ExchangeFilterFunction` 实现请求日志、鉴权签名、埋点。
  - 支持多配置源（YAML、环境变量、Apollo/Nacos）统一注入 Base URL、超时、重试策略。
- **错误处理**：
  - 自定义 `ResponseErrorHandler`，对 4xx/5xx 解析成领域异常。
  - Reactor `retryWhen` 策略，区分幂等与非幂等调用。
- **序列化**：
  - 使用 `Jackson2ObjectMapperBuilder` 定制：时间格式、空值策略、枚举序列化。
  - 提供 `JsonBodyBuilder`、`FormDataBuilder` 工具，适配多媒体与 JSON。

### 调用类型示例

1. **同步调用** (`Mono.block`)
   - 典型业务流程：AI Agent 下游配置同步接口，要求阻塞返回。
   - 示例：`UserProfileClient#getProfileSync(String userId)`。
   - 考虑超时控制：`timeout(Duration.ofSeconds(x))` + 错误重试。

2. **异步调用** (`Mono`/`Flux`)
   - 提供 `CompletableFuture`/`Mono` 等形式供上层以响应式串联。
   - 示例：`RecommendationClient#fetchAsync(RequestDto request)`。
   - 支持并发聚合、`zip`/`flatMap` 组合。

3. **响应式流式调用** (`Flux` stream)
   - 处理流式 JSON/NDJSON 或分块传输。
   - 示例：`ContentStreamClient#consumeContentFlux()`，结合背压策略。

4. **SSE（Server-Sent Events）调用**
   - `MediaType.TEXT_EVENT_STREAM`，解析 `ServerSentEvent<T>`。
  - 示例：`NotificationClient#subscribeNotifications()`，支持自动重连、心跳。

5. **MCP 调用**（面向 Model Context Protocol）
   - **SSE 版本**：遵循 MCP SSE handshake (`Content-Type: text/event-stream`)，解析消息头中的 `event`, `id`, `data` 字段。
   - **Streamable HTTP**：利用 HTTP chunked + `application/json`，每块体为 MCP 消息结构，结合 Reactor 解码。
   - 提供 `McpClient` 封装 handshake、消息序列号、异常转换。

### 企业级最佳实践

- **统一鉴权**：支持 Bearer Token、API Key、HMAC，使用 `ExchangeFilterFunction` 注入。
- **审计与追踪**：
  - MDC 透传 `traceId`, `spanId`。
  - 可选集成 Zipkin/OTel。
- **幂等控制**：对 `POST` 请求提供 `Idempotency-Key` 头策略。
- **熔断限流**：与 Resilience4j 集成 fallback，示例提供注释或配置。
- **国际化与错误码**：定义 `ErrorResponse` 标准结构，统一处理。

## 数据模型与持久层

- **实体定义**：`User`, `Recommendation`, `StreamMessage` 等示例实体。
- **持久层**：
  - MyBatis 映射文件示例 (`resources/mapper/*.xml`)。
  - MyBatis-Plus 基础 CRUD Mapper，演示如何配合 WebClient 调用结果落库。
- **数据源**：默认 H2 + Schema 初始化脚本，便于测试；支持扩展至 MySQL。

## 测试策略

- **测试框架**：JUnit 5 + Spring Boot Test。
- **测试类型**：
  - 单元测试：针对 `WebClient` 封装类，使用 `MockWebServer`/`WireMock`。
  - 集成测试：`@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate` 或 `WebTestClient`。
  - 响应式测试：`StepVerifier` 验证 `Mono`/`Flux`。
  - SSE 与 MCP：利用 `SseEmitter`/`MockMvc` 或自定义模拟器，确保断开重连逻辑。
- **覆盖要求**：
  - 每种调用方式提供对应测试类，如：
    - `SyncClientTests`
    - `AsyncClientTests`
    - `StreamingClientTests`
    - `SseClientTests`
    - `McpSseClientTests`
    - `McpStreamClientTests`
  - 测试数据放置 `src/test/resources/data/*.json`。

## 配置与运维

- **配置文件**：
  - `application.yml`：多 profile (`dev`, `test`, `prod`)；示范不同环境下的外部服务地址。
  - 规划敏感信息通过 `application-{env}.yaml` + 环境变量覆盖。
- **日志**：
  - `logback-spring.xml`：调用链日志 + JSON 输出选项。

## 文档与交付物

- **README**：项目简介、快速启动、调用示例、测试运行方式。
- **API 文档**：以 Markdown 输出各示例 API 调用说明。
- **配置说明**：`docs/configuration.md` 描述配置项、默认值、安全建议。
- **架构图**：`docs/architecture.puml` 使用 PlantUML 绘制模块与流程。
- **开发规范**：
  - 提交的示例代码与测试需包含充分的行级注释，解释关键 WebClient 配置、操作符及错误处理策略。
  - 所有对外暴露的类、接口与方法必须提供详细 Javadoc，说明使用场景、参数、返回值与异常。
  - 覆盖测试时在断言前后补充注释，帮助读者理解预期行为与依赖假设。

## 后续扩展建议（暂不实现）

- 引入 `spring-cloud-contract` 保障下游接口契约。
- 集成认证中心（OAuth2 Client Credentials）示例。
- 增加 gRPC / GraphQL WebClient 适配。
- 提供 Kotlin DSL 版本或 Scala 化示例，便于多语言 AI Agent 调用。
- 构建统一 Dev Portal，将文档、测试 Mock、调试工具整合。
