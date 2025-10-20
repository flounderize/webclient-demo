package com.example.webclient.support;

import com.example.webclient.support.exception.RemoteServiceException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

/**
 * WebClient 错误处理器，将 4xx/5xx 转换为领域异常。
 */
public class WebClientErrorHandler implements ExchangeFilterFunction {

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        return next.exchange(request).flatMap(response -> {
            if (response.statusCode().isError()) {
                return response.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .flatMap(body -> Mono.error(toException(request, response, body)));
            }
            return Mono.just(response);
        });
    }

    private Throwable toException(ClientRequest request, ClientResponse response, String body) {
        HttpStatusCode statusCode = response.statusCode();
        return new RemoteServiceException(
            statusCode.value(),
            "外部服务调用失败: %s %s 响应码 %d".formatted(request.method(), request.url(), statusCode.value()),
            body
        );
    }

    /**
     * 将响应体解析为指定类型并抛出异常。
     *
     * @param response ClientResponse
     * @param typeReference 类型引用
     * @param <T> 类型参数
     * @return Mono<T>
     */
    public <T> Mono<T> readBody(ClientResponse response, ParameterizedTypeReference<T> typeReference) {
        return response.bodyToMono(typeReference)
            .switchIfEmpty(Mono.error(new RemoteServiceException(
                response.statusCode().value(),
                "响应体为空",
                ""
            )));
    }
}
