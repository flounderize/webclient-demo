package com.example.webclient.config;

import com.example.webclient.filter.LoggingExchangeFilterFunction;
import com.example.webclient.filter.AuthenticationExchangeFilterFunction;
import com.example.webclient.filter.TracingExchangeFilterFunction;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * WebClient 配置类
 * 
 * <p>提供统一的 WebClient 配置，包括：
 * <ul>
 *   <li>连接池配置</li>
 *   <li>超时设置</li>
 *   <li>请求/响应日志</li>
 *   <li>鉴权过滤器</li>
 *   <li>追踪过滤器</li>
 *   <li>序列化配置</li>
 * </ul>
 * 
 * @author AI Agent
 * @since 1.0.0
 */
@Configuration
public class WebClientConfig {

    @Value("${webclient.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${webclient.connection-timeout:5000}")
    private int connectionTimeout;

    @Value("${webclient.read-timeout:30000}")
    private int readTimeout;

    @Value("${webclient.write-timeout:30000}")
    private int writeTimeout;

    @Value("${webclient.max-connections:500}")
    private int maxConnections;

    @Value("${webclient.max-idle-time:20}")
    private int maxIdleTime;

    @Value("${webclient.max-memory-size:16777216}")
    private int maxMemorySize;

    /**
     * 创建默认的 WebClient.Builder Bean
     * 
     * <p>配置了连接池、超时、过滤器链等，可以被注入到各个客户端类中使用
     * 
     * @param objectMapper Jackson ObjectMapper
     * @param loggingFilter 日志过滤器
     * @param authFilter 认证过滤器
     * @param tracingFilter 追踪过滤器
     * @return 配置好的 WebClient.Builder
     */
    @Bean
    public WebClient.Builder webClientBuilder(
            ObjectMapper objectMapper,
            LoggingExchangeFilterFunction loggingFilter,
            AuthenticationExchangeFilterFunction authFilter,
            TracingExchangeFilterFunction tracingFilter) {

        // 配置连接池
        ConnectionProvider connectionProvider = ConnectionProvider.builder("custom")
                .maxConnections(maxConnections)
                .maxIdleTime(Duration.ofSeconds(maxIdleTime))
                .maxLifeTime(Duration.ofMinutes(5))
                .pendingAcquireTimeout(Duration.ofSeconds(60))
                .evictInBackground(Duration.ofSeconds(120))
                .build();

        // 配置 HttpClient
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(writeTimeout, TimeUnit.MILLISECONDS)))
                .responseTimeout(Duration.ofMillis(readTimeout));

        // 配置 ExchangeStrategies（序列化配置）
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> {
                    // 设置最大内存大小（默认 256KB，这里设置为 16MB）
                    configurer.defaultCodecs().maxInMemorySize(maxMemorySize);
                    
                    // 使用自定义的 ObjectMapper，支持 JSON 和 NDJSON
                    configurer.defaultCodecs().jackson2JsonEncoder(
                            new Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON, MediaType.APPLICATION_NDJSON));
                    configurer.defaultCodecs().jackson2JsonDecoder(
                            new Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON, MediaType.APPLICATION_NDJSON));
                })
                .build();

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                // 过滤器链：追踪 -> 认证 -> 日志
                .filter(tracingFilter)
                .filter(authFilter)
                .filter(loggingFilter);
    }

    /**
     * 创建默认的 WebClient Bean
     * 
     * @param builder WebClient.Builder
     * @return WebClient 实例
     */
    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }

    /**
     * 创建用于 SSE 调用的 WebClient Bean
     * 
     * <p>针对 Server-Sent Events 的特殊配置：
     * <ul>
     *   <li>接受 text/event-stream 媒体类型</li>
     *   <li>更长的读取超时时间</li>
     *   <li>禁用连接超时（保持长连接）</li>
     * </ul>
     * 
     * @param objectMapper Jackson ObjectMapper
     * @param loggingFilter 日志过滤器
     * @param tracingFilter 追踪过滤器
     * @return 用于 SSE 的 WebClient 实例
     */
    @Bean
    public WebClient sseWebClient(
            ObjectMapper objectMapper,
            LoggingExchangeFilterFunction loggingFilter,
            TracingExchangeFilterFunction tracingFilter) {

        // SSE 专用连接池：更少的连接数，更长的保持时间
        ConnectionProvider sseConnectionProvider = ConnectionProvider.builder("sse")
                .maxConnections(50)
                .maxIdleTime(Duration.ofMinutes(10))
                .maxLifeTime(Duration.ofHours(1))
                .build();

        // SSE 专用 HttpClient：无响应超时
        HttpClient sseHttpClient = HttpClient.create(sseConnectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(0, TimeUnit.MILLISECONDS))) // 0 表示无超时
                .responseTimeout(Duration.ZERO); // 无响应超时

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> {
                    configurer.defaultCodecs().maxInMemorySize(maxMemorySize);
                    configurer.defaultCodecs().jackson2JsonEncoder(
                            new Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON, MediaType.APPLICATION_NDJSON));
                    configurer.defaultCodecs().jackson2JsonDecoder(
                            new Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON, MediaType.APPLICATION_NDJSON));
                })
                .build();

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(sseHttpClient))
                .exchangeStrategies(strategies)
                .defaultHeader(HttpHeaders.ACCEPT, "text/event-stream")
                .filter(tracingFilter)
                .filter(loggingFilter)
                .build();
    }
}
