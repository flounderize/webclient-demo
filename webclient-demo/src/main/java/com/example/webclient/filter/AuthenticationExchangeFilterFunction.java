package com.example.webclient.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

/**
 * 认证过滤器
 * 
 * <p>支持多种认证方式：
 * <ul>
 *   <li>Bearer Token</li>
 *   <li>API Key</li>
 *   <li>Basic Auth</li>
 * </ul>
 * 
 * <p>认证信息从配置文件读取，可以根据不同的环境（dev/test/prod）配置不同的认证方式
 * 
 * @author AI Agent
 * @since 1.0.0
 */
@Component
public class AuthenticationExchangeFilterFunction implements ExchangeFilterFunction {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationExchangeFilterFunction.class);

    @Value("${webclient.auth.type:none}")
    private String authType;

    @Value("${webclient.auth.token:}")
    private String token;

    @Value("${webclient.auth.api-key:}")
    private String apiKey;

    @Value("${webclient.auth.api-key-header:X-API-Key}")
    private String apiKeyHeader;

    @Value("${webclient.auth.username:}")
    private String username;

    @Value("${webclient.auth.password:}")
    private String password;

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        ClientRequest authenticatedRequest = switch (authType.toLowerCase()) {
            case "bearer" -> addBearerToken(request);
            case "apikey" -> addApiKey(request);
            case "basic" -> addBasicAuth(request);
            default -> request; // 无认证
        };

        return next.exchange(authenticatedRequest);
    }

    /**
     * 添加 Bearer Token 认证
     */
    private ClientRequest addBearerToken(ClientRequest request) {
        if (token == null || token.isEmpty()) {
            log.warn("Bearer token not configured");
            return request;
        }

        log.debug("Adding Bearer token authentication");
        return ClientRequest.from(request)
                .header("Authorization", "Bearer " + token)
                .build();
    }

    /**
     * 添加 API Key 认证
     */
    private ClientRequest addApiKey(ClientRequest request) {
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("API key not configured");
            return request;
        }

        log.debug("Adding API key authentication with header: {}", apiKeyHeader);
        return ClientRequest.from(request)
                .header(apiKeyHeader, apiKey)
                .build();
    }

    /**
     * 添加 Basic Auth 认证
     */
    private ClientRequest addBasicAuth(ClientRequest request) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            log.warn("Basic auth credentials not configured");
            return request;
        }

        log.debug("Adding Basic authentication");
        return ClientRequest.from(request)
                .headers(headers -> headers.setBasicAuth(username, password))
                .build();
    }
}
