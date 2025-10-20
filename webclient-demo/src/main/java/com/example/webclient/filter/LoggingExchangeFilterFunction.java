package com.example.webclient.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

/**
 * 日志记录过滤器
 * 
 * <p>记录 WebClient 请求和响应的详细信息，包括：
 * <ul>
 *   <li>请求方法和 URL</li>
 *   <li>请求头</li>
 *   <li>响应状态码</li>
 *   <li>响应头</li>
 *   <li>请求耗时</li>
 * </ul>
 * 
 * @author AI Agent
 * @since 1.0.0
 */
@Component
public class LoggingExchangeFilterFunction implements ExchangeFilterFunction {

    private static final Logger log = LoggerFactory.getLogger(LoggingExchangeFilterFunction.class);

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        long startTime = System.currentTimeMillis();
        
        // 记录请求信息
        logRequest(request);
        
        return next.exchange(request)
                .doOnNext(response -> {
                    // 记录响应信息
                    long duration = System.currentTimeMillis() - startTime;
                    logResponse(request, response, duration);
                })
                .doOnError(error -> {
                    // 记录错误信息
                    long duration = System.currentTimeMillis() - startTime;
                    logError(request, error, duration);
                });
    }

    /**
     * 记录请求详情
     */
    private void logRequest(ClientRequest request) {
        if (log.isDebugEnabled()) {
            log.debug("WebClient Request: {} {}", request.method(), request.url());
            log.debug("Request Headers: {}", request.headers());
        }
    }

    /**
     * 记录响应详情
     */
    private void logResponse(ClientRequest request, ClientResponse response, long duration) {
        if (log.isDebugEnabled()) {
            log.debug("WebClient Response: {} {} - Status: {} - Duration: {}ms",
                    request.method(), request.url(), response.statusCode(), duration);
            log.debug("Response Headers: {}", response.headers().asHttpHeaders());
        } else if (log.isInfoEnabled()) {
            log.info("WebClient: {} {} - {} - {}ms",
                    request.method(), request.url(), response.statusCode(), duration);
        }
    }

    /**
     * 记录错误信息
     */
    private void logError(ClientRequest request, Throwable error, long duration) {
        log.error("WebClient Error: {} {} - Duration: {}ms - Error: {}",
                request.method(), request.url(), duration, error.getMessage(), error);
    }
}
