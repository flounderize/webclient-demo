package com.example.webclient.config;

import com.example.webclient.support.IdempotencyKeyGenerator;
import com.example.webclient.support.WebClientAuthFilter;
import com.example.webclient.support.WebClientErrorHandler;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

/**
 * WebClient 配置，统一构建具备鉴权、日志、错误处理与重试能力的客户端工厂。
 */
@Configuration
@EnableConfigurationProperties(WebClientProperties.class)
public class WebClientConfiguration {

    /**
     * 提供全局 WebClient.Builder，按需自定义过滤器与底层资源池。
     *
     * @param properties 属性定义
     * @param authFilter 鉴权过滤器
     * @param errorHandler 错误处理
     * @return WebClient.Builder
     */
    @Bean
    @ConditionalOnMissingBean
    public WebClient.Builder webClientBuilder(WebClientProperties properties,
                                              WebClientAuthFilter authFilter,
                                              WebClientErrorHandler errorHandler) {
        WebClientProperties.ServiceProperties defaults = properties.getDefaults();
        ConnectionProvider provider = ConnectionProvider.builder("webclient-demo")
            .maxConnections(200)
            .pendingAcquireMaxCount(1000)
            .pendingAcquireTimeout(Duration.ofSeconds(2))
            .build();
        HttpClient httpClient = HttpClient.create(provider)
            .responseTimeout(defaults.getReadTimeout())
            .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS,
                Math.toIntExact(defaults.getConnectTimeout().toMillis()));

        ExchangeStrategies strategies = ExchangeStrategies.builder()
            .codecs(codecs -> codecs.defaultCodecs()
                .maxInMemorySize(defaults.getMaxInMemorySize()))
            .build();

        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .exchangeStrategies(strategies)
            .filter(authFilter)
            .filter(logRequest())
            .filter(errorHandler)
            .filter(idempotencyFilter());
    }

    /**
     * 默认鉴权过滤器。
     *
     * @param properties 配置项
     * @return WebClientAuthFilter
     */
    @Bean
    @ConditionalOnMissingBean
    public WebClientAuthFilter webClientAuthFilter(WebClientProperties properties) {
        return new WebClientAuthFilter(properties);
    }

    /**
     * 默认错误处理过滤器。
     *
     * @return WebClientErrorHandler
     */
    @Bean
    @ConditionalOnMissingBean
    public WebClientErrorHandler webClientErrorHandler() {
        return new WebClientErrorHandler();
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            ClientRequest mutated = ClientRequest.from(request)
                .header("X-Trace-Id", IdempotencyKeyGenerator.generateTraceId())
                .build();
            return Mono.just(mutated);
        });
    }

    private ExchangeFilterFunction idempotencyFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            if ("POST".equalsIgnoreCase(clientRequest.method().name())) {
                ClientRequest mutated = ClientRequest.from(clientRequest)
                    .header("Idempotency-Key", IdempotencyKeyGenerator.generate())
                    .build();
                return Mono.just(mutated);
            }
            return Mono.just(clientRequest);
        });
    }

    /**
     * 根据不同服务配置创建命名 WebClient。
     *
     * @param builder builder
     * @param properties 属性
     * @return WebClientRegistry
     */
    @Bean
    public WebClientRegistry webClientRegistry(WebClient.Builder builder, WebClientProperties properties) {
        return new WebClientRegistry(builder, properties);
    }

    /**
     * WebClient 注册表，缓存不同服务的 WebClient 实例。
     */
    public static class WebClientRegistry {
        private final WebClient.Builder builder;
        private final WebClientProperties properties;
        private final Map<String, WebClient> cache = new java.util.concurrent.ConcurrentHashMap<>();

        public WebClientRegistry(WebClient.Builder builder, WebClientProperties properties) {
            this.builder = builder;
            this.properties = properties;
        }

        /**
         * 按服务名获取 WebClient。
         *
         * @param serviceName 服务名称
         * @return WebClient 实例
         */
        public WebClient get(String serviceName) {
            return cache.computeIfAbsent(serviceName, key -> {
                WebClientProperties.ServiceProperties sp = properties.getServices() != null
                    ? properties.getServices().getOrDefault(key, properties.getDefaults())
                    : properties.getDefaults();
                WebClient.Builder serviceBuilder = builder.clone();
                if (sp.getBaseUrl() != null) {
                    serviceBuilder.baseUrl(sp.getBaseUrl());
                }
                return serviceBuilder.build();
            });
        }

        /**
         * 获取默认 WebClient。
         *
         * @return WebClient
         */
        public WebClient getDefault() {
            return get("default");
        }
    }
}
