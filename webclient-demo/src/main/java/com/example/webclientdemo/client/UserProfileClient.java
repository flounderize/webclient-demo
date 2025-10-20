package com.example.webclientdemo.client;

import com.example.webclientdemo.config.WebClientConfiguration.WebClientRegistry;
import com.example.webclientdemo.model.User;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * 同步用户档案客户端，演示阻塞式调用及超时处理。
 */
@Component
public class UserProfileClient {

    private final WebClient client;

    public UserProfileClient(WebClientRegistry registry) {
        this.client = registry.get("user-service");
    }

    /**
     * 同步获取用户信息，示例中直接阻塞。
     *
     * @param userId 用户 ID
     * @return 用户对象
     */
    public User getProfileSync(String userId) {
        return client.get()
            .uri(uriBuilder -> uriBuilder.path("/api/users/{id}").build(userId))
            .retrieve()
            .bodyToMono(User.class)
            .timeout(java.time.Duration.ofSeconds(3))
            .retryWhen(configureRetry())
            .block();
    }

    private reactor.util.retry.Retry configureRetry() {
        return reactor.util.retry.Retry.backoff(2, java.time.Duration.ofMillis(200))
            .filter(throwable -> throwable instanceof com.example.webclientdemo.support.exception.RemoteServiceException)
            .onRetryExhaustedThrow((spec, signal) -> signal.failure());
    }

    /**
     * 异步方式获取用户信息，供响应式链路使用。
     *
     * @param userId 用户 ID
     * @return Mono<User>
     */
    public Mono<User> getProfileAsync(String userId) {
        return client.get()
            .uri(uriBuilder -> uriBuilder.path("/api/users/{id}").build(userId))
            .retrieve()
            .bodyToMono(User.class)
            .timeout(java.time.Duration.ofSeconds(3));
    }
}
