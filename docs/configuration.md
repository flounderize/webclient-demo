# WebClient 配置说明

## 1. 配置文件概述

项目使用 `application.yml` 作为主配置文件，支持多环境配置（dev、test、prod）。

## 2. WebClient 核心配置

### 2.1 基础配置

```yaml
webclient:
  # 基础 URL - 所有请求的默认前缀
  base-url: http://localhost:8080
  
  # 连接超时时间（毫秒）- 建立连接的最大等待时间
  connection-timeout: 5000
  
  # 读取超时时间（毫秒）- 读取响应数据的最大等待时间
  read-timeout: 30000
  
  # 写入超时时间（毫秒）- 发送请求数据的最大等待时间
  write-timeout: 30000
  
  # 最大连接数 - 连接池的最大连接数
  max-connections: 500
  
  # 最大空闲时间（秒）- 连接在池中的最大空闲时间
  max-idle-time: 20
  
  # 最大内存大小（字节）- 编解码器缓冲区的最大大小
  # 默认 256KB，这里设置为 16MB
  max-memory-size: 16777216
```

### 2.2 配置项说明

#### connection-timeout

- **含义**：建立 TCP 连接的最大等待时间
- **默认值**：5000ms (5秒)
- **建议值**：
  - 内网服务：2000-5000ms
  - 公网服务：5000-10000ms
- **影响**：设置过短可能导致连接失败；设置过长会长时间占用线程

#### read-timeout

- **含义**：从服务器读取响应的最大等待时间
- **默认值**：30000ms (30秒)
- **建议值**：
  - 快速接口：5000-10000ms
  - 普通接口：10000-30000ms
  - 慢查询接口：30000-60000ms
  - 流式/SSE：0（无超时）或很长的时间
- **影响**：设置过短可能导致正常请求超时；设置过长会长时间占用资源

#### max-connections

- **含义**：连接池的最大连接数
- **默认值**：500
- **建议值**：
  - 小型应用：100-200
  - 中型应用：200-500
  - 大型应用：500-1000
- **计算公式**：`max-connections = (线程数 × 2) + 预留数`
- **影响**：设置过小会限制并发；设置过大会占用过多系统资源

#### max-idle-time

- **含义**：连接在池中的最大空闲时间
- **默认值**：20秒
- **建议值**：10-60秒
- **影响**：设置过短会频繁创建连接；设置过长可能保持无效连接

#### max-memory-size

- **含义**：编解码器缓冲区的最大大小
- **默认值**：256KB (262144 字节)
- **建议值**：
  - 小型响应：256KB (默认)
  - 中型响应：1-4MB
  - 大型响应：4-16MB
  - 超大响应：使用流式传输，不要增大此值
- **影响**：设置过小会导致大响应解析失败；设置过大会占用过多内存

## 3. 认证配置

### 3.1 配置示例

```yaml
webclient:
  auth:
    # 认证类型：none, bearer, apikey, basic
    type: none
    
    # Bearer Token 认证
    token: ""
    
    # API Key 认证
    api-key: ""
    api-key-header: X-API-Key
    
    # Basic 认证
    username: ""
    password: ""
```

### 3.2 认证类型说明

#### None（无认证）

```yaml
auth:
  type: none
```

适用于：
- 公开 API
- 开发环境测试
- 内网无需认证的服务

#### Bearer Token

```yaml
auth:
  type: bearer
  token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

适用于：
- OAuth 2.0 / JWT 认证
- 现代 RESTful API
- 微服务间认证

请求头格式：
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### API Key

```yaml
auth:
  type: apikey
  api-key: your-secret-api-key
  api-key-header: X-API-Key
```

适用于：
- 第三方 API 服务
- 简单的认证场景
- 服务端 API

请求头格式：
```
X-API-Key: your-secret-api-key
```

#### Basic Auth

```yaml
auth:
  type: basic
  username: admin
  password: secret
```

适用于：
- 传统 HTTP Basic 认证
- 内部管理接口
- 简单的用户名密码认证

请求头格式：
```
Authorization: Basic YWRtaW46c2VjcmV0
```

## 4. 多环境配置

### 4.1 开发环境（dev）

```yaml
spring:
  config:
    activate:
      on-profile: dev

webclient:
  base-url: http://localhost:8080
  auth:
    type: none

logging:
  level:
    com.example.webclient: DEBUG
```

**特点**：
- 本地服务地址
- 无认证或测试认证
- 详细的日志输出

**启动命令**：
```bash
java -jar app.jar --spring.profiles.active=dev
```

### 4.2 测试环境（test）

```yaml
spring:
  config:
    activate:
      on-profile: test

webclient:
  base-url: http://test-api.example.com
  auth:
    type: apikey
    api-key: test-api-key

logging:
  level:
    com.example.webclient: INFO
```

**特点**：
- 测试环境服务地址
- 测试环境认证凭据
- 适中的日志级别

**启动命令**：
```bash
java -jar app.jar --spring.profiles.active=test
```

### 4.3 生产环境（prod）

```yaml
spring:
  config:
    activate:
      on-profile: prod

webclient:
  base-url: http://api.example.com
  auth:
    type: bearer
    token: ${API_TOKEN:}

logging:
  level:
    root: WARN
    com.example.webclient: INFO
```

**特点**：
- 生产环境服务地址
- 使用环境变量注入敏感信息
- 精简的日志输出

**启动命令**：
```bash
export API_TOKEN=production-token
java -jar app.jar --spring.profiles.active=prod
```

## 5. 日志配置

### 5.1 日志级别

```yaml
logging:
  level:
    root: INFO                                          # 根日志级别
    com.example.webclient: DEBUG                        # 应用日志级别
    org.springframework.web.reactive.function.client: DEBUG  # WebClient 日志
```

### 5.2 日志格式

```yaml
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### 5.3 日志级别说明

- **TRACE**: 最详细，包含所有操作的详细信息
- **DEBUG**: 调试信息，适用于开发环境
- **INFO**: 一般信息，适用于生产环境
- **WARN**: 警告信息，可能的问题
- **ERROR**: 错误信息，需要关注的问题

## 6. 数据源配置

### 6.1 H2 数据库（开发/测试）

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password:
    
  h2:
    console:
      enabled: true
      path: /h2-console
```

### 6.2 MySQL 数据库（生产）

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/webclient_demo
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
```

## 7. Actuator 配置

### 7.1 端点配置

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
```

### 7.2 端点说明

- **/actuator/health**: 健康检查
- **/actuator/info**: 应用信息
- **/actuator/metrics**: 性能指标
- **/actuator/prometheus**: Prometheus 格式的指标

## 8. 环境变量覆盖

### 8.1 常用环境变量

```bash
# WebClient 基础配置
export WEBCLIENT_BASE_URL=http://api.example.com
export WEBCLIENT_CONNECTION_TIMEOUT=5000
export WEBCLIENT_READ_TIMEOUT=30000

# 认证配置
export WEBCLIENT_AUTH_TYPE=bearer
export WEBCLIENT_AUTH_TOKEN=your-token

# 数据库配置
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/db
export SPRING_DATASOURCE_USERNAME=user
export SPRING_DATASOURCE_PASSWORD=pass

# 日志配置
export LOGGING_LEVEL_COM_EXAMPLE_WEBCLIENT=INFO
```

### 8.2 命令行参数

```bash
java -jar app.jar \
  --webclient.base-url=http://api.example.com \
  --webclient.auth.type=bearer \
  --webclient.auth.token=your-token \
  --spring.profiles.active=prod
```

## 9. 配置优先级

Spring Boot 配置优先级（从高到低）：

1. 命令行参数
2. 环境变量
3. application-{profile}.yml
4. application.yml
5. 默认值

## 10. 安全建议

### 10.1 敏感信息处理

❌ **不要**在配置文件中硬编码敏感信息：

```yaml
# 错误示例
webclient:
  auth:
    token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

✅ **应该**使用环境变量：

```yaml
# 正确示例
webclient:
  auth:
    token: ${API_TOKEN:}
```

### 10.2 配置文件加密

对于生产环境，建议使用：

- Spring Cloud Config + Vault
- Jasypt 加密
- Kubernetes Secrets
- AWS Secrets Manager

### 10.3 最小权限原则

- 不同环境使用不同的认证凭据
- 定期轮换 Token/API Key
- 限制 Token 的权限范围

## 11. 配置验证

### 11.1 启动时验证

在应用启动时验证关键配置：

```java
@Component
public class ConfigurationValidator implements ApplicationRunner {
    @Value("${webclient.base-url}")
    private String baseUrl;
    
    @Override
    public void run(ApplicationArguments args) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalStateException("webclient.base-url must be configured");
        }
    }
}
```

### 11.2 配置检查清单

- [ ] base-url 已配置且可访问
- [ ] 超时配置合理
- [ ] 连接池大小适当
- [ ] 认证信息正确
- [ ] 日志级别适合当前环境
- [ ] 敏感信息未硬编码

## 12. 故障排查

### 12.1 连接超时

**现象**：`ConnectTimeoutException`

**检查**：
- 网络是否可达
- 防火墙是否开放
- DNS 解析是否正常
- `connection-timeout` 配置是否合理

### 12.2 读取超时

**现象**：`ReadTimeoutException`

**检查**：
- 服务端是否响应过慢
- `read-timeout` 配置是否足够
- 是否需要增加超时时间或优化服务端

### 12.3 连接池耗尽

**现象**：`PoolAcquireTimeoutException`

**检查**：
- `max-connections` 是否足够
- 是否有连接泄漏（未正确释放）
- 并发请求数是否超出预期

### 12.4 内存溢出

**现象**：`DataBufferLimitException`

**检查**：
- 响应体是否过大
- `max-memory-size` 是否需要增大
- 考虑使用流式传输

## 13. 性能调优建议

### 13.1 连接池调优

```yaml
webclient:
  max-connections: 500          # 根据并发量调整
  max-idle-time: 20             # 根据请求频率调整
```

### 13.2 超时调优

```yaml
webclient:
  connection-timeout: 3000      # 内网服务可以更短
  read-timeout: 10000           # 根据接口响应时间调整
```

### 13.3 缓冲区调优

```yaml
webclient:
  max-memory-size: 16777216     # 根据响应大小调整
```

## 14. 参考资料

- [Spring Boot Configuration Properties](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html)
- [Reactor Netty Reference](https://projectreactor.io/docs/netty/release/reference/)
- [Spring Boot Externalized Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
