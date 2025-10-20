package com.example.webclientdemo.support;

import com.example.webclientdemo.config.WebClientProperties;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpHeaders;
import org.springframework.util.DigestUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

/**
 * 鉴权过滤器，根据配置自动在请求头中添加凭证。
 */
public class WebClientAuthFilter implements ExchangeFilterFunction {

    private final WebClientProperties properties;
    private final Map<String, WebClientProperties.AuthProperties> cache = new ConcurrentHashMap<>();

    public WebClientAuthFilter(WebClientProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        ClientRequest.Builder builder = ClientRequest.from(request);
        WebClientProperties.ServiceProperties serviceProperties = resolveServiceProperties(request);
        applyAuth(serviceProperties.getAuth(), request, builder);
        return next.exchange(builder.build());
    }

    private WebClientProperties.ServiceProperties resolveServiceProperties(ClientRequest request) {
        String host = request.url().getHost();
        if (properties.getServices() == null) {
            return properties.getDefaults();
        }
        return properties.getServices().values().stream()
            .filter(service -> service.getBaseUrl() != null && service.getBaseUrl().contains(host))
            .findFirst()
            .orElse(properties.getDefaults());
    }

    private void applyAuth(WebClientProperties.AuthProperties auth, ClientRequest original, ClientRequest.Builder builder) {
        if (auth == null || "none".equalsIgnoreCase(auth.getType())) {
            return;
        }
        if ("bearer".equalsIgnoreCase(auth.getType())) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + auth.getToken());
            return;
        }
        if ("api-key".equalsIgnoreCase(auth.getType())) {
            builder.header(auth.getApiKeyHeader(), auth.getApiKeyValue());
            return;
        }
        if ("hmac".equalsIgnoreCase(auth.getType())) {
            String nonce = IdempotencyKeyGenerator.generate("nonce");
            String timestamp = OffsetDateTime.now().toString();
            String content = original.method().name() + "\n" + timestamp + "\n" + nonce;
            String signature = DigestUtils.md5DigestAsHex((content + auth.getHmacSecret()).getBytes(StandardCharsets.UTF_8));
            builder.header("X-Sign-Nonce", nonce)
                .header("X-Sign-Time", timestamp)
                .header("X-Signature", Base64.getEncoder().encodeToString(signature.getBytes(StandardCharsets.UTF_8)));
        }
    }

    /**
     * 用于缓存特定服务的鉴权配置（可扩展刷新机制）。
     *
     * @param serviceName 服务名称
     * @return 鉴权配置
     */
    public WebClientProperties.AuthProperties getCachedAuth(String serviceName) {
        return cache.computeIfAbsent(serviceName, key -> properties.getServices() != null
            ? properties.getServices().getOrDefault(key, properties.getDefaults()).getAuth()
            : properties.getDefaults().getAuth());
    }
}
